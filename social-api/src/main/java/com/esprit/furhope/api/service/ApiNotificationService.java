package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.NotificationDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiNotificationService {

    private final JdbcTemplate jdbcTemplate;

    public ApiNotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NotificationDto> getLatest(int recipientId, int limit) {
        int safeLimit = limit <= 0 ? 30 : limit;
        String sql = "SELECT n.*, u.name AS actor_name " +
                "FROM notification n " +
                "JOIN api_user u ON u.id_user = n.actor_id " +
                "WHERE n.recipient_id = ? " +
                "ORDER BY n.created_at DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            NotificationDto dto = new NotificationDto();
            dto.setId(rs.getLong("id"));
            dto.setActorId(rs.getLong("actor_id"));
            dto.setActorName(rs.getString("actor_name"));
            dto.setType(rs.getString("type"));
            dto.setMessage(rs.getString("message"));
            dto.setRead(rs.getBoolean("is_read"));
            dto.setCreatedAt(rs.getTimestamp("created_at"));

            long postVal = rs.getLong("post_id");
            dto.setPostId(rs.wasNull() ? null : postVal);

            long commentVal = rs.getLong("comment_id");
            dto.setCommentId(rs.wasNull() ? null : commentVal);
            return dto;
        }, recipientId, safeLimit);
    }

    public int countUnread(int recipientId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE recipient_id = ? AND is_read = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, recipientId);
        return count == null ? 0 : count;
    }

    public void markAllRead(int recipientId) {
        String sql = "UPDATE notification SET is_read = 1 WHERE recipient_id = ? AND is_read = 0";
        jdbcTemplate.update(sql, recipientId);
    }
}
