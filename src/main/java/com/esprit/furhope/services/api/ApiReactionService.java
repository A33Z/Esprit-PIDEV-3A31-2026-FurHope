package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;
import com.esprit.furhope.integration.api.dto.ReactionDto;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ApiReactionService {

    private final ApiClient apiClient = new ApiClient();

    public String getReaction(long postId, long userId) throws SQLException {
        try {
            ReactionDto dto = apiClient.get("/api/posts/" + postId + "/reaction", Map.of("userId", userId), ReactionDto.class);
            if (dto == null) return null;
            return normalize(dto.getReaction());
        } catch (IOException e) {
            throw asSqlException("Failed to load reaction", e);
        }
    }

    public String toggleReaction(long postId, long userId, String desired) throws SQLException {
        try {
            ReactionDto dto = apiClient.post(
                    "/api/posts/" + postId + "/reaction",
                    null,
                    Map.of("userId", userId, "reaction", desired),
                    ReactionDto.class
            );
            if (dto == null) return null;
            return normalize(dto.getReaction());
        } catch (IOException e) {
            throw asSqlException("Failed to toggle reaction", e);
        }
    }

    private String normalize(String reaction) {
        if (reaction == null) return null;
        if ("NONE".equalsIgnoreCase(reaction)) return null;
        return reaction;
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
