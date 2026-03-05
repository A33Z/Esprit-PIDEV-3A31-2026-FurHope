package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationServiceJdbc extends ConnectToDbService {

    public static class Notif {
        private final long id;
        private final int actorId;
        private final String actorName;
        private final String actorProfileImagePath;
        private final String type;
        private final String message;
        private final boolean isRead;
        private final Timestamp createdAt;
        private final Long postId;
        private final Long commentId;

        public Notif(long id, int actorId, String actorName, String actorProfileImagePath, String type, String message, boolean isRead,
                     Timestamp createdAt, Long postId, Long commentId) {
            this.id = id;
            this.actorId = actorId;
            this.actorName = actorName;
            this.actorProfileImagePath = actorProfileImagePath;
            this.type = type;
            this.message = message;
            this.isRead = isRead;
            this.createdAt = createdAt;
            this.postId = postId;
            this.commentId = commentId;
        }

        public long getId() {
            return id;
        }

        public int getActorId() {
            return actorId;
        }

        public String getActorName() {
            return actorName;
        }

        public String getActorProfileImagePath() {
            return actorProfileImagePath;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public boolean isRead() {
            return isRead;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }

        public Long getPostId() {
            return postId;
        }

        public Long getCommentId() {
            return commentId;
        }
    }

    public NotificationServiceJdbc() {
        super();
    }

    public void createNotification(int recipientId, int actorId, String type, Long postId, Long commentId, String message) throws SQLException {
        if (recipientId == actorId) {
            return;
        }

        String sql = "INSERT INTO notification(recipient_id, actor_id, type, post_id, comment_id, message) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            ps.setInt(2, actorId);
            ps.setString(3, type);
            if (postId == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, postId);
            }
            if (commentId == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, commentId);
            }
            ps.setString(6, message);
            ps.executeUpdate();
        }
    }

    public List<Notif> getLatest(int recipientId, int limit) throws SQLException {
        int safeLimit = limit <= 0 ? 30 : limit;
        String sql = "SELECT n.*, " +
                "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''), " +
                "u.email, CONCAT('User ', n.actor_id)) AS actor_name, " +
                "u.profile_image_path AS actor_profile_image_path " +
                "FROM notification n " +
                "LEFT JOIN `user` u ON u.id = n.actor_id " +
                "WHERE n.recipient_id = ? " +
                "ORDER BY n.created_at DESC " +
                "LIMIT ?";

        List<Notif> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int actorId = rs.getInt("actor_id");
                    String actorName = rs.getString("actor_name");
                    String actorProfileImagePath = rs.getString("actor_profile_image_path");
                    String type = rs.getString("type");
                    String message = rs.getString("message");
                    boolean isRead = rs.getBoolean("is_read");
                    Timestamp createdAt = rs.getTimestamp("created_at");

                    long postVal = rs.getLong("post_id");
                    Long postId = rs.wasNull() ? null : postVal;

                    long commentVal = rs.getLong("comment_id");
                    Long commentId = rs.wasNull() ? null : commentVal;

                    result.add(new Notif(id, actorId, actorName, actorProfileImagePath, type, message, isRead, createdAt, postId, commentId));
                }
            }
        }
        return result;
    }

    public int countUnread(int recipientId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM notification WHERE recipient_id = ? AND is_read = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("c");
                }
                return 0;
            }
        }
    }

    public void markAllRead(int recipientId) throws SQLException {
        String sql = "UPDATE notification SET is_read = 1 WHERE recipient_id = ? AND is_read = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            ps.executeUpdate();
        }
    }

    public void markAsRead(long notificationId, int recipientId) throws SQLException {
        String sql = "UPDATE notification SET is_read = 1 WHERE id = ? AND recipient_id = ? AND is_read = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            ps.setInt(2, recipientId);
            ps.executeUpdate();
        }
    }
}
