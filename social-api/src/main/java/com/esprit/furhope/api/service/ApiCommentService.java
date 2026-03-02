package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.CommentDto;
import com.esprit.furhope.api.dto.CreateCommentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ApiCommentService {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public ApiCommentService(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
    }

    public List<CommentDto> getPostComments(long postId) {
        String sql = "SELECT c.id, c.post_id, c.author_id, u.name AS author_name, c.body, c.parent_comment_id, c.created_at " +
                "FROM comment c " +
                "LEFT JOIN api_user u ON u.id_user = c.author_id " +
                "WHERE c.post_id = ? " +
                "ORDER BY c.created_at ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapComment(rs), postId);
    }

    @Transactional
    public CommentDto createComment(long postId, CreateCommentRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String insertSql = "INSERT INTO comment (post_id, author_id, parent_comment_id, body, status) " +
                "VALUES (:postId, :authorId, :parentCommentId, :body, :status)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("authorId", request.getUserId())
                .addValue("parentCommentId", request.getParentCommentId())
                .addValue("body", request.getContent().trim())
                .addValue("status", "ACTIVE");

        namedJdbc.update(insertSql, params, keyHolder, new String[]{"id"});
        Number generated = keyHolder.getKey();
        if (generated == null) throw new IllegalStateException("Failed to create comment");
        long commentId = generated.longValue();

        jdbcTemplate.update(
                "UPDATE post SET comments_count = GREATEST(comments_count + 1, 0) WHERE id = ?",
                postId
        );

        Long postOwnerId = findPostOwnerId(postId);
        createNotification(postOwnerId, request.getUserId(), "POST_COMMENT", postId, commentId, "New comment on your post");

        if (request.getParentCommentId() != null) {
            Long parentAuthorId = findCommentAuthorId(request.getParentCommentId());
            createNotification(parentAuthorId, request.getUserId(), "COMMENT_REPLY", postId, commentId, "replied to your comment");
        }

        return getCommentById(commentId);
    }

    public boolean updateComment(long commentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
        int rows = jdbcTemplate.update(
                "UPDATE comment SET body=?, status=? WHERE id=?",
                content.trim(), "ACTIVE", commentId
        );
        return rows > 0;
    }

    @Transactional
    public boolean deleteComment(long commentId) {
        Long postId = jdbcTemplate.query(
                "SELECT post_id FROM comment WHERE id = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong("post_id") : null,
                commentId
        );
        if (postId == null) return false;

        Integer totalDeleted = jdbcTemplate.query(
                "WITH RECURSIVE comment_tree AS (" +
                        "    SELECT id FROM comment WHERE id = ? " +
                        "    UNION ALL " +
                        "    SELECT c.id FROM comment c " +
                        "    INNER JOIN comment_tree t ON c.parent_comment_id = t.id" +
                        ") " +
                        "SELECT COUNT(*) AS total FROM comment_tree",
                rs -> rs.next() ? rs.getInt("total") : 1,
                commentId
        );

        int rows = jdbcTemplate.update(
                "WITH RECURSIVE comment_tree AS (" +
                        "    SELECT id FROM comment WHERE id = ? " +
                        "    UNION ALL " +
                        "    SELECT c.id FROM comment c " +
                        "    INNER JOIN comment_tree t ON c.parent_comment_id = t.id" +
                        ") " +
                        "DELETE FROM comment WHERE id IN (SELECT id FROM comment_tree)",
                commentId
        );
        if (rows <= 0) return false;

        jdbcTemplate.update(
                "UPDATE post SET comments_count = GREATEST(comments_count - ?, 0) WHERE id = ?",
                totalDeleted == null ? 1 : totalDeleted, postId
        );
        return true;
    }

    public boolean deleteByPostId(long postId) {
        jdbcTemplate.update("DELETE FROM comment WHERE post_id = ?", postId);
        jdbcTemplate.update("UPDATE post SET comments_count = 0 WHERE id = ?", postId);
        return true;
    }

    private CommentDto getCommentById(long commentId) {
        String sql = "SELECT c.id, c.post_id, c.author_id, u.name AS author_name, c.body, c.parent_comment_id, c.created_at " +
                "FROM comment c " +
                "LEFT JOIN api_user u ON u.id_user = c.author_id " +
                "WHERE c.id = ? LIMIT 1";
        return jdbcTemplate.query(sql, rs -> rs.next() ? mapComment(rs) : null, commentId);
    }

    private CommentDto mapComment(ResultSet rs) throws SQLException {
        CommentDto dto = new CommentDto();
        dto.setId(rs.getLong("id"));
        dto.setPostId(rs.getLong("post_id"));
        dto.setAuthorId(rs.getLong("author_id"));
        dto.setAuthorName(rs.getString("author_name"));
        dto.setContent(rs.getString("body"));
        long parent = rs.getLong("parent_comment_id");
        dto.setParentCommentId(rs.wasNull() ? null : parent);
        dto.setCreatedAt(rs.getTimestamp("created_at"));
        return dto;
    }

    private Long findPostOwnerId(long postId) {
        return jdbcTemplate.query(
                "SELECT author_id FROM post WHERE id = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong("author_id") : null,
                postId
        );
    }

    private Long findCommentAuthorId(long commentId) {
        return jdbcTemplate.query(
                "SELECT author_id FROM comment WHERE id = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong("author_id") : null,
                commentId
        );
    }

    private void createNotification(Long recipientId, long actorId, String type, long postId, long commentId, String message) {
        if (recipientId == null) return;
        if (recipientId == actorId) return;
        try {
            jdbcTemplate.update(
                    "INSERT INTO notification(recipient_id, actor_id, type, post_id, comment_id, message) VALUES (?, ?, ?, ?, ?, ?)",
                    recipientId, actorId, type, postId, commentId, message
            );
        } catch (Exception ignored) {
            // Notification insert must not break core comment flow.
        }
    }
}
