package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ApiShareService {

    private final ApiClient apiClient = new ApiClient();

    public boolean hasShared(long postId, long userId) throws SQLException {
        try {
            Map<String, Object> result = apiClient.get("/api/posts/" + postId + "/share", Map.of("userId", userId), Map.class);
            Object shared = result == null ? null : result.get("shared");
            return shared instanceof Boolean && (Boolean) shared;
        } catch (IOException e) {
            throw asSqlException("Failed to check share status", e);
        }
    }

    public boolean toggleShare(long postId, long userId) throws SQLException {
        try {
            Map<String, Object> result = apiClient.post("/api/posts/" + postId + "/share", null, Map.of("userId", userId), Map.class);
            Object shared = result == null ? null : result.get("shared");
            return shared instanceof Boolean && (Boolean) shared;
        } catch (IOException e) {
            throw asSqlException("Failed to toggle share", e);
        }
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
