package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;
import com.esprit.furhope.integration.api.dto.CountDto;
import com.esprit.furhope.integration.api.dto.NotificationDto;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiNotificationService {

    public static class Notif {
        private final long id;
        private final int actorId;
        private final String actorName;
        private final String type;
        private final String message;
        private final boolean isRead;
        private final Timestamp createdAt;
        private final Long postId;
        private final Long commentId;

        public Notif(long id, int actorId, String actorName, String type, String message, boolean isRead,
                     Timestamp createdAt, Long postId, Long commentId) {
            this.id = id;
            this.actorId = actorId;
            this.actorName = actorName;
            this.type = type;
            this.message = message;
            this.isRead = isRead;
            this.createdAt = createdAt;
            this.postId = postId;
            this.commentId = commentId;
        }

        public long getId() { return id; }
        public int getActorId() { return actorId; }
        public String getActorName() { return actorName; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public boolean isRead() { return isRead; }
        public Timestamp getCreatedAt() { return createdAt; }
        public Long getPostId() { return postId; }
        public Long getCommentId() { return commentId; }
    }

    private final ApiClient apiClient = new ApiClient();

    public List<Notif> getLatest(int recipientId, int limit) throws SQLException {
        try {
            List<NotificationDto> dtos = apiClient.get(
                    "/api/notifications",
                    Map.of("userId", recipientId, "limit", limit),
                    new TypeReference<List<NotificationDto>>() {
                    }
            );
            List<Notif> out = new ArrayList<>();
            if (dtos == null) return out;
            for (NotificationDto d : dtos) {
                out.add(new Notif(d.getId(), d.getActorId(), d.getActorName(), d.getType(), d.getMessage(), d.isRead(),
                        d.getCreatedAt(), d.getPostId(), d.getCommentId()));
            }
            return out;
        } catch (IOException e) {
            throw asSqlException("Failed to load notifications", e);
        }
    }

    public int countUnread(int recipientId) throws SQLException {
        try {
            CountDto dto = apiClient.get(
                    "/api/notifications/unreadCount",
                    Map.of("userId", recipientId),
                    CountDto.class
            );
            return dto == null ? 0 : dto.getCount();
        } catch (IOException e) {
            throw asSqlException("Failed to count unread notifications", e);
        }
    }

    public void markAllRead(int recipientId) throws SQLException {
        try {
            apiClient.post("/api/notifications/markAllRead", null, Map.of("userId", recipientId), Map.class);
        } catch (IOException e) {
            throw asSqlException("Failed to mark notifications read", e);
        }
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
