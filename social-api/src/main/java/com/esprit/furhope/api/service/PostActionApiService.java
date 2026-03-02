package com.esprit.furhope.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostActionApiService {

    private final JdbcTemplate jdbcTemplate;

    public PostActionApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasShared(long postId, long userId) {
        Integer one = jdbcTemplate.query(
                "SELECT 1 FROM post_share WHERE post_id=? AND user_id=? LIMIT 1",
                rs -> rs.next() ? 1 : null,
                postId, userId
        );
        return one != null;
    }

    @Transactional
    public boolean toggleShare(long postId, long userId) {
        if (!hasShared(postId, userId)) {
            jdbcTemplate.update("INSERT INTO post_share(post_id, user_id) VALUES (?, ?)", postId, userId);
            jdbcTemplate.update("UPDATE post SET shares_count = shares_count + 1 WHERE id = ?", postId);
            return true;
        }

        jdbcTemplate.update("DELETE FROM post_share WHERE post_id=? AND user_id=?", postId, userId);
        jdbcTemplate.update("UPDATE post SET shares_count = GREATEST(shares_count - 1, 0) WHERE id = ?", postId);
        return false;
    }

    public boolean hasReported(long postId, long userId) {
        Integer one = jdbcTemplate.query(
                "SELECT 1 FROM post_report WHERE post_id=? AND reporter_user_id=? LIMIT 1",
                rs -> rs.next() ? 1 : null,
                postId, userId
        );
        return one != null;
    }

    public boolean reportPost(long postId, long userId, String reason) {
        if (hasReported(postId, userId)) return false;

        int rows = jdbcTemplate.update(
                "INSERT INTO post_report(post_id, reporter_user_id, reason) VALUES (?, ?, ?)",
                postId, userId, (reason == null || reason.isBlank()) ? null : reason.trim()
        );
        return rows > 0;
    }
}
