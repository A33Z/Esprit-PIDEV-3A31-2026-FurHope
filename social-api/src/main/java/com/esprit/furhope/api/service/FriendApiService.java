package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.FriendDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendApiService {

    private final JdbcTemplate jdbcTemplate;

    public FriendApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean sendRequest(long senderId, long receiverId) {
        if (senderId == receiverId) return false;
        if (areFriends(senderId, receiverId)) return false;
        if (hasPendingRequestEitherDirection(senderId, receiverId)) return false;

        int rows = jdbcTemplate.update(
                "INSERT INTO friend_request(sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')",
                senderId, receiverId
        );
        return rows > 0;
    }

    @Transactional
    public boolean acceptRequest(long receiverId, long senderId) {
        int updated = jdbcTemplate.update(
                "UPDATE friend_request SET status='ACCEPTED' " +
                        "WHERE sender_id=? AND receiver_id=? AND status='PENDING'",
                senderId, receiverId
        );
        if (updated == 0) return false;

        long user1 = Math.min(senderId, receiverId);
        long user2 = Math.max(senderId, receiverId);
        if (!friendshipExists(user1, user2)) {
            jdbcTemplate.update(
                    "INSERT INTO friendship(user1_id, user2_id) VALUES (?, ?)",
                    user1, user2
            );
        }
        return true;
    }

    public List<FriendDto> getFriends(long userId) {
        String sql = "SELECT u.id_user, u.name " +
                "FROM friendship f " +
                "JOIN api_user u ON u.id_user = f.user2_id " +
                "WHERE f.user1_id = ? " +
                "UNION " +
                "SELECT u.id_user, u.name " +
                "FROM friendship f " +
                "JOIN api_user u ON u.id_user = f.user1_id " +
                "WHERE f.user2_id = ? " +
                "ORDER BY name ASC";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new FriendDto(rs.getLong("id_user"), rs.getString("name")),
                userId, userId
        );
    }

    public boolean declineRequest(long receiverId, long senderId) {
        int rows = jdbcTemplate.update(
                "UPDATE friend_request SET status='DECLINED' WHERE sender_id=? AND receiver_id=? AND status='PENDING'",
                senderId, receiverId
        );
        return rows > 0;
    }

    public List<FriendDto> getIncomingRequests(long userId) {
        String sql = "SELECT u.id_user, u.name " +
                "FROM friend_request fr " +
                "JOIN api_user u ON u.id_user = fr.sender_id " +
                "WHERE fr.receiver_id = ? AND fr.status='PENDING' " +
                "ORDER BY fr.created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FriendDto(rs.getLong("id_user"), rs.getString("name")), userId);
    }

    public List<FriendDto> searchUsersByName(long userId, String query) {
        String sql = "SELECT id_user, name FROM api_user WHERE id_user <> ? AND name LIKE ? ORDER BY name ASC LIMIT 10";
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FriendDto(rs.getLong("id_user"), rs.getString("name")), userId, q);
    }

    private boolean hasPendingRequestEitherDirection(long userA, long userB) {
        Integer one = jdbcTemplate.query(
                "SELECT 1 FROM friend_request " +
                        "WHERE ((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)) " +
                        "AND status='PENDING' LIMIT 1",
                rs -> rs.next() ? 1 : null,
                userA, userB, userB, userA
        );
        return one != null;
    }

    private boolean areFriends(long userA, long userB) {
        long user1 = Math.min(userA, userB);
        long user2 = Math.max(userA, userB);
        return friendshipExists(user1, user2);
    }

    private boolean friendshipExists(long user1, long user2) {
        Integer one = jdbcTemplate.query(
                "SELECT 1 FROM friendship WHERE user1_id=? AND user2_id=? LIMIT 1",
                rs -> rs.next() ? 1 : null,
                user1, user2
        );
        return one != null;
    }
}
