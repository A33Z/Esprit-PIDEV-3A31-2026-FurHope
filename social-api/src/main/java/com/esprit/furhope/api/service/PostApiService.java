package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.CreatePostRequest;
import com.esprit.furhope.api.dto.PostDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class PostApiService {

    private final JdbcTemplate jdbcTemplate;

    public PostApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PostDto create(CreatePostRequest req) {
        String sql = "INSERT INTO post (author_id, caption, media_type, media_path, thumbnail_path, duration_seconds, visibility, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, req.getAuthorId());
            ps.setString(2, req.getCaption());
            ps.setString(3, req.getMediaType());
            ps.setString(4, req.getMediaPath());
            ps.setString(5, req.getThumbnailPath());
            if (req.getDurationSeconds() == null) ps.setNull(6, java.sql.Types.INTEGER);
            else ps.setInt(6, req.getDurationSeconds());
            ps.setString(7, req.getVisibility());
            ps.setString(8, req.getStatus());
            return ps;
        }, kh);

        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("Failed to create post");
        return findById(key.longValue());
    }

    public boolean update(long id, CreatePostRequest req) {
        int rows = jdbcTemplate.update(
                "UPDATE post SET caption=?, media_type=?, media_path=?, thumbnail_path=?, duration_seconds=?, visibility=?, status=? WHERE id=?",
                req.getCaption(),
                req.getMediaType(),
                req.getMediaPath(),
                req.getThumbnailPath(),
                req.getDurationSeconds(),
                req.getVisibility(),
                req.getStatus(),
                id
        );
        return rows > 0;
    }

    public boolean delete(long id) {
        return jdbcTemplate.update("DELETE FROM post WHERE id = ?", id) > 0;
    }

    public Long getFirstPostId() {
        return jdbcTemplate.query(
                "SELECT id FROM post ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getLong("id") : null
        );
    }

    public PostDto findById(long id) {
        String sql = "SELECT p.*, u.name AS author_name FROM post p LEFT JOIN api_user u ON u.id_user = p.author_id WHERE p.id = ? LIMIT 1";
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            PostDto dto = new PostDto();
            dto.setId(rs.getLong("id"));
            dto.setAuthorId(rs.getInt("author_id"));
            dto.setAuthorName(rs.getString("author_name"));
            dto.setCaption(rs.getString("caption"));
            dto.setMediaType(rs.getString("media_type"));
            dto.setMediaPath(rs.getString("media_path"));
            dto.setThumbnailPath(rs.getString("thumbnail_path"));
            int d = rs.getInt("duration_seconds");
            dto.setDurationSeconds(rs.wasNull() ? null : d);
            dto.setLikesCount(rs.getInt("likes_count"));
            dto.setDislikesCount(rs.getInt("dislikes_count"));
            dto.setSharesCount(rs.getInt("shares_count"));
            dto.setCommentsCount(rs.getInt("comments_count"));
            dto.setVisibility(rs.getString("visibility"));
            dto.setStatus(rs.getString("status"));
            dto.setCreatedAt(rs.getTimestamp("created_at"));
            dto.setUpdatedAt(rs.getTimestamp("updated_at"));
            return dto;
        }, id);
    }
}
