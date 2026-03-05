package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendServiceJdbc extends ConnectToDbService {

    private final NotificationServiceJdbc notifSvc = new NotificationServiceJdbc();

    public static class UserMini {
        private final int id;
        private final String name;
        private final String profileImagePath;

        public UserMini(int id, String name, String profileImagePath) {
            this.id = id;
            this.name = name;
            this.profileImagePath = profileImagePath;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getProfileImagePath() {
            return profileImagePath;
        }
    }

    public FriendServiceJdbc() {
        super();
    }

    public boolean sendRequest(int senderId, int receiverId) throws SQLException {
        if (senderId <= 0 || receiverId <= 0 || senderId == receiverId) {
            return false;
        }
        if (areFriends(senderId, receiverId)) {
            return false;
        }
        if (hasPendingRequestEitherDirection(senderId, receiverId)) {
            return false;
        }

        String sql = "INSERT INTO friend_request(sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            boolean created = ps.executeUpdate() > 0;
            if (created) {
                safeNotify(receiverId, senderId, "FRIEND_REQUEST_SENT", null, "sent you a friend request");
            }
            return created;
        }
    }

    public boolean addFriend(int senderId, int receiverId) throws SQLException {
        return sendRequest(senderId, receiverId);
    }

    public boolean acceptRequest(int receiverId, int senderId) throws SQLException {
        con.setAutoCommit(false);
        try {
            String updateRequest = "UPDATE friend_request SET status='ACCEPTED' " +
                    "WHERE sender_id=? AND receiver_id=? AND status='PENDING'";
            int updated;
            try (PreparedStatement ps = con.prepareStatement(updateRequest)) {
                ps.setInt(1, senderId);
                ps.setInt(2, receiverId);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                con.rollback();
                return false;
            }

            int user1 = Math.min(senderId, receiverId);
            int user2 = Math.max(senderId, receiverId);
            if (!friendshipExists(user1, user2)) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO friendship(user1_id, user2_id) VALUES (?, ?)")) {
                    ps.setInt(1, user1);
                    ps.setInt(2, user2);
                    ps.executeUpdate();
                }
            }

            con.commit();
            safeNotify(senderId, receiverId, "FRIEND_REQUEST_ACCEPTED", null, "accepted your friend request");
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public boolean declineRequest(int receiverId, int senderId) throws SQLException {
        String sql = "UPDATE friend_request SET status='DECLINED' " +
                "WHERE sender_id=? AND receiver_id=? AND status='PENDING'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            boolean declined = ps.executeUpdate() > 0;
            if (declined) {
                safeNotify(senderId, receiverId, "FRIEND_REQUEST_DECLINED", null, "declined your friend request");
            }
            return declined;
        }
    }

    public boolean removeFriend(int userA, int userB) throws SQLException {
        int user1 = Math.min(userA, userB);
        int user2 = Math.max(userA, userB);

        con.setAutoCommit(false);
        try {
            int deleted;
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM friendship WHERE user1_id=? AND user2_id=?")) {
                ps.setInt(1, user1);
                ps.setInt(2, user2);
                deleted = ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE friend_request SET status='CANCELLED' " +
                            "WHERE ((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)) " +
                            "AND status='PENDING'")) {
                ps.setInt(1, userA);
                ps.setInt(2, userB);
                ps.setInt(3, userB);
                ps.setInt(4, userA);
                ps.executeUpdate();
            }

            con.commit();
            if (deleted > 0) {
                safeNotify(userB, userA, "FRIEND_REMOVED", null, "removed you from friends");
            }
            return deleted > 0;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public List<UserMini> getFriends(int userId) throws SQLException {
        String sql = "SELECT u.id, " + userDisplaySql("u") + " AS name, u.profile_image_path AS profile_image_path " +
                "FROM friendship f " +
                "JOIN `user` u ON u.id = f.user2_id " +
                "WHERE f.user1_id = ? " +
                "UNION " +
                "SELECT u.id, " + userDisplaySql("u") + " AS name, u.profile_image_path AS profile_image_path " +
                "FROM friendship f " +
                "JOIN `user` u ON u.id = f.user1_id " +
                "WHERE f.user2_id = ? " +
                "ORDER BY name ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            return mapUsers(ps);
        }
    }

    public List<UserMini> listFriends(int userId) throws SQLException {
        return getFriends(userId);
    }

    public List<UserMini> getIncomingRequests(int userId) throws SQLException {
        String sql = "SELECT u.id, " + userDisplaySql("u") + " AS name, u.profile_image_path AS profile_image_path " +
                "FROM friend_request fr " +
                "JOIN `user` u ON u.id = fr.sender_id " +
                "WHERE fr.receiver_id = ? AND fr.status='PENDING' " +
                "ORDER BY fr.created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return mapUsers(ps);
        }
    }

    public List<UserMini> listFriendRequests(int userId) throws SQLException {
        return getIncomingRequests(userId);
    }

    public List<UserMini> searchUsersByName(String query, int excludeUserId) throws SQLException {
        String q = query == null ? "" : query.trim();
        String sql = "SELECT id, " + userDisplaySql("u") + " AS name, u.profile_image_path AS profile_image_path " +
                "FROM `user` u " +
                "WHERE id <> ? " +
                "AND (" +
                "CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')) LIKE ? " +
                "OR email LIKE ?" +
                ") " +
                "ORDER BY name ASC " +
                "LIMIT 10";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, excludeUserId);
            ps.setString(2, "%" + q + "%");
            ps.setString(3, "%" + q + "%");
            return mapUsers(ps);
        }
    }

    public boolean hasPendingRequest(int userA, int userB) throws SQLException {
        return hasPendingRequestEitherDirection(userA, userB);
    }

    private List<UserMini> mapUsers(PreparedStatement ps) throws SQLException {
        List<UserMini> users = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new UserMini(rs.getInt("id"), rs.getString("name"), rs.getString("profile_image_path")));
            }
        }
        return users;
    }

    private boolean hasPendingRequestEitherDirection(int userA, int userB) throws SQLException {
        String sql = "SELECT 1 FROM friend_request " +
                "WHERE ((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)) " +
                "AND status='PENDING' LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean areFriends(int userA, int userB) throws SQLException {
        int user1 = Math.min(userA, userB);
        int user2 = Math.max(userA, userB);
        return friendshipExists(user1, user2);
    }

    private boolean friendshipExists(int user1, int user2) throws SQLException {
        String sql = "SELECT 1 FROM friendship WHERE user1_id=? AND user2_id=? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, user1);
            ps.setInt(2, user2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String userDisplaySql(String alias) {
        return "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(" + alias + ".first_name, ''), ' ', COALESCE(" + alias + ".last_name, ''))), ''), " +
                alias + ".email, CONCAT('User ', " + alias + ".id))";
    }

    private void safeNotify(int recipientId, int actorId, String type, Long postId, String message) {
        try {
            notifSvc.createNotification(recipientId, actorId, type, postId, null, message);
        } catch (SQLException ignored) {
            // Keep friend flows successful even if notification insertion fails.
        }
    }
}
