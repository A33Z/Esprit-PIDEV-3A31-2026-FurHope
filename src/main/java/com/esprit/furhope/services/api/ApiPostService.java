package com.esprit.furhope.services.api;

import com.esprit.furhope.entities.post;
import com.esprit.furhope.integration.api.ApiClient;
import com.esprit.furhope.integration.api.dto.ApiResult;
import com.esprit.furhope.integration.api.dto.PostDto;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiPostService {

    private final ApiClient apiClient = new ApiClient();

    public void ajouter(post p) throws SQLException {
        try {
            PostDto dto = apiClient.post("/api/posts", null, toCreateRequest(p), PostDto.class);
            if (dto != null) {
                p.setId(dto.getId());
            }
        } catch (IOException e) {
            throw asSqlException("Failed to create post", e);
        }
    }

    public void supprimer(long id) throws SQLException {
        try {
            apiClient.delete("/api/posts/" + id, null, ApiResult.class);
        } catch (IOException e) {
            throw asSqlException("Failed to delete post", e);
        }
    }

    public List<post> afficher() throws SQLException {
        return new ArrayList<>();
    }

    public List<post> afficher(long currentUserId) throws SQLException {
        try {
            List<PostDto> rows = apiClient.get(
                    "/api/posts/feed",
                    Map.of("userId", currentUserId),
                    new TypeReference<List<PostDto>>() {
                    }
            );
            List<post> posts = new ArrayList<>();
            if (rows != null) {
                for (PostDto row : rows) {
                    posts.add(toEntity(row));
                }
            }
            return posts;
        } catch (IOException e) {
            throw asSqlException("Failed to load feed", e);
        }
    }

    public void modifier(post p) throws SQLException {
        try {
            apiClient.put("/api/posts/" + p.getId(), null, toCreateRequest(p), ApiResult.class);
        } catch (IOException e) {
            throw asSqlException("Failed to update post", e);
        }
    }

    public Long getFirstPostId() throws SQLException {
        try {
            Map<String, Long> result = apiClient.get(
                    "/api/posts/firstId",
                    null,
                    new TypeReference<Map<String, Long>>() {
                    }
            );
            if (result == null) return null;
            Long id = result.get("id");
            return (id == null || id < 0) ? null : id;
        } catch (IOException e) {
            throw asSqlException("Failed to fetch first post id", e);
        }
    }

    private Map<String, Object> toCreateRequest(post p) {
        Map<String, Object> body = new HashMap<>();
        body.put("authorId", p.getAuthorId());
        body.put("caption", p.getCaption());
        body.put("mediaType", p.getMediaType());
        body.put("mediaPath", p.getMediaPath());
        body.put("thumbnailPath", p.getThumbnailPath());
        body.put("durationSeconds", p.getDurationSeconds());
        body.put("visibility", p.getVisibility());
        body.put("status", p.getStatus());
        return body;
    }

    private post toEntity(PostDto dto) {
        post p = new post();
        p.setId(dto.getId());
        p.setAuthorId(dto.getAuthorId());
        p.setAuthorName(dto.getAuthorName());
        p.setCaption(dto.getCaption());
        p.setMediaType(dto.getMediaType());
        p.setMediaPath(dto.getMediaPath());
        p.setThumbnailPath(dto.getThumbnailPath());
        p.setDurationSeconds(dto.getDurationSeconds());
        p.setLikesCount(dto.getLikesCount());
        p.setDislikesCount(dto.getDislikesCount());
        p.setSharesCount(dto.getSharesCount());
        p.setCommentsCount(dto.getCommentsCount());
        p.setVisibility(dto.getVisibility());
        p.setStatus(dto.getStatus());
        p.setCreatedAt(dto.getCreatedAt());
        p.setUpdatedAt(dto.getUpdatedAt());
        return p;
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }
}
