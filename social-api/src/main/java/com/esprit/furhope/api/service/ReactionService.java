package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.ReactionDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionService {

    private final JdbcTemplate jdbcTemplate;

    public ReactionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getUserReaction(long postId, long userId) {
        String sql = "SELECT reaction FROM post_reaction WHERE post_id=? AND user_id=? LIMIT 1";
        String reaction = jdbcTemplate.query(
                sql,
                rs -> rs.next() ? rs.getString("reaction") : null,
                postId, userId
        );
        return reaction == null ? "NONE" : reaction.toUpperCase();
    }

    @Transactional
    public ReactionDto toggleReaction(long postId, long userId, String reaction) {
        String desired = normalizeReaction(reaction);
        String current = getCurrentReaction(postId, userId);

        if (current == null) {
            jdbcTemplate.update(
                    "INSERT INTO post_reaction(post_id,user_id,reaction) VALUES (?,?,?)",
                    postId, userId, desired
            );
            incrementCounter(postId, desired, 1);
            return new ReactionDto(postId, userId, desired);
        }

        if (current.equalsIgnoreCase(desired)) {
            jdbcTemplate.update("DELETE FROM post_reaction WHERE post_id=? AND user_id=?", postId, userId);
            incrementCounter(postId, current, -1);
            return new ReactionDto(postId, userId, "NONE");
        }

        jdbcTemplate.update(
                "UPDATE post_reaction SET reaction=? WHERE post_id=? AND user_id=?",
                desired, postId, userId
        );
        incrementCounter(postId, current, -1);
        incrementCounter(postId, desired, 1);
        return new ReactionDto(postId, userId, desired);
    }

    private String getCurrentReaction(long postId, long userId) {
        String sql = "SELECT reaction FROM post_reaction WHERE post_id=? AND user_id=? LIMIT 1";
        return jdbcTemplate.query(
                sql,
                rs -> rs.next() ? rs.getString("reaction") : null,
                postId, userId
        );
    }

    private void incrementCounter(long postId, String reaction, int delta) {
        String normalized = normalizeReaction(reaction);
        if ("LIKE".equals(normalized)) {
            if (delta >= 0) {
                jdbcTemplate.update("UPDATE post SET likes_count = likes_count + ? WHERE id = ?", delta, postId);
            } else {
                jdbcTemplate.update("UPDATE post SET likes_count = GREATEST(likes_count + ?, 0) WHERE id = ?", delta, postId);
            }
            return;
        }

        if (delta >= 0) {
            jdbcTemplate.update("UPDATE post SET dislikes_count = dislikes_count + ? WHERE id = ?", delta, postId);
        } else {
            jdbcTemplate.update("UPDATE post SET dislikes_count = GREATEST(dislikes_count + ?, 0) WHERE id = ?", delta, postId);
        }
    }

    private String normalizeReaction(String reaction) {
        if (reaction == null) throw new IllegalArgumentException("reaction is required");
        String normalized = reaction.trim().toUpperCase();
        if (!"LIKE".equals(normalized) && !"DISLIKE".equals(normalized)) {
            throw new IllegalArgumentException("reaction must be LIKE or DISLIKE");
        }
        return normalized;
    }
}
