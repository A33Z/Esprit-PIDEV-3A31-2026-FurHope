package com.esprit.furhope.services.api;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.integration.api.ApiClient;
import com.esprit.furhope.integration.api.dto.ApiResult;
import com.esprit.furhope.integration.api.dto.CommentDto;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiCommentService {

    private final ApiClient apiClient = new ApiClient();

    public void ajouter(comment c) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", c.getAuthorId());
        body.put("content", c.getBody());
        body.put("parentCommentId", c.getParentCommentId());

        try {
            CommentDto dto = apiClient.post("/api/posts/" + c.getPostId() + "/comments", null, body, CommentDto.class);
            if (dto != null) {
                c.setId(dto.getId());
                c.setCreatedAt(dto.getCreatedAt());
                c.setAuthorName(dto.getAuthorName());
            }
        } catch (IOException e) {
            throw asSqlException("Failed to add comment", e);
        }
    }

    public void supprimer(long id) throws SQLException {
        try {
            apiClient.delete("/api/posts/comments/" + id, null, ApiResult.class);
        } catch (IOException e) {
            throw asSqlException("Failed to delete comment", e);
        }
    }

    public List<comment> afficher() throws SQLException {
        return new ArrayList<>();
    }

    public void modifier(comment c) throws SQLException {
        Map<String, Object> body = Map.of("content", c.getBody());
        try {
            apiClient.put("/api/posts/comments/" + c.getId(), null, body, ApiResult.class);
        } catch (IOException e) {
            throw asSqlException("Failed to update comment", e);
        }
    }

    public List<comment> findByPostId(long postId) throws SQLException {
        try {
            List<CommentDto> rows = apiClient.get(
                    "/api/posts/" + postId + "/comments",
                    null,
                    new TypeReference<List<CommentDto>>() {
                    }
            );

            List<comment> out = new ArrayList<>();
            if (rows != null) {
                for (CommentDto row : rows) {
                    out.add(toEntity(row));
                }
            }
            return out;
        } catch (IOException e) {
            throw asSqlException("Failed to load comments", e);
        }
    }

    public void deleteByPostId(long postId) throws SQLException {
        try {
            apiClient.delete("/api/posts/" + postId + "/comments", null, ApiResult.class);
        } catch (IOException e) {
            throw asSqlException("Failed to delete post comments", e);
        }
    }

    private comment toEntity(CommentDto dto) {
        comment c = new comment();
        c.setId(dto.getId());
        c.setPostId(dto.getPostId());
        c.setAuthorId((int) dto.getAuthorId());
        c.setAuthorName(dto.getAuthorName());
        c.setBody(dto.getContent());
        c.setParentCommentId(dto.getParentCommentId());
        c.setCreatedAt(dto.getCreatedAt());
        c.setStatus("ACTIVE");
        return c;
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
