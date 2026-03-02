package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.PostDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class PostFeedService {

    private final JdbcTemplate jdbcTemplate;

    public PostFeedService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostDto> getFeed(long viewerId) {
        String sql = "SELECT p.*, u.name AS author_name " +
                "FROM api_post p " +
                "LEFT JOIN api_user u ON u.id_user = p.author_id " +
                "WHERE p.status = ? " +
                "AND (" +
                "p.visibility = ? " +
                "OR (p.visibility = ? AND p.author_id = ?) " +
                "OR (p.visibility = ? AND (" +
                "p.author_id = ? " +
                "OR EXISTS (" +
                "SELECT 1 FROM friendship f " +
                "WHERE f.user1_id = LEAST(p.author_id, ?) " +
                "AND f.user2_id = GREATEST(p.author_id, ?)" +
                ")" +
                "))" +
                ") " +
                "ORDER BY p.created_at DESC";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapPost(rs),
                "ACTIVE",
                "PUBLIC",
                "PRIVATE",
                viewerId,
                "FRIENDS",
                viewerId,
                viewerId,
                viewerId
        );
    }

    private PostDto mapPost(ResultSet rs) throws SQLException {
        PostDto dto = new PostDto();
        dto.setId(rs.getLong("id"));
        dto.setAuthorId(rs.getInt("author_id"));
        dto.setAuthorName(rs.getString("author_name"));
        dto.setCaption(rs.getString("caption"));
        dto.setMediaType(rs.getString("media_type"));
        dto.setMediaPath(rs.getString("media_path"));
        dto.setThumbnailPath(rs.getString("thumbnail_path"));

        int duration = rs.getInt("duration_seconds");
        dto.setDurationSeconds(rs.wasNull() ? null : duration);

        dto.setLikesCount(rs.getInt("likes_count"));
        dto.setDislikesCount(rs.getInt("dislikes_count"));
        dto.setSharesCount(rs.getInt("shares_count"));
        dto.setCommentsCount(rs.getInt("comments_count"));
        dto.setVisibility(rs.getString("visibility"));
        dto.setStatus(rs.getString("status"));
        dto.setCreatedAt(rs.getTimestamp("created_at"));
        dto.setUpdatedAt(rs.getTimestamp("updated_at"));
        return dto;
    }
}
