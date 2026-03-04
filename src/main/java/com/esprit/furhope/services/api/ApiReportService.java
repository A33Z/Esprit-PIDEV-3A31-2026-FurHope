package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ApiReportService {

    private final ApiClient apiClient = new ApiClient();

    public boolean hasReported(long postId, long reporterUserId) throws SQLException {
        try {
            Map<String, Object> result = apiClient.get(
                    "/api/posts/" + postId + "/report/status",
                    Map.of("userId", reporterUserId),
                    Map.class
            );
            Object reported = result == null ? null : result.get("reported");
            return reported instanceof Boolean && (Boolean) reported;
        } catch (IOException e) {
            throw asSqlException("Failed to check report status", e);
        }
    }

    public boolean reportPost(long postId, long reporterUserId, String reason) throws SQLException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", reporterUserId);
            body.put("reason", reason);
            Map<String, Object> result = apiClient.post(
                    "/api/posts/" + postId + "/report",
                    null,
                    body,
                    Map.class
            );
            Object success = result == null ? null : result.get("success");
            return success instanceof Boolean && (Boolean) success;
        } catch (IOException e) {
            throw asSqlException("Failed to report post", e);
        }
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
