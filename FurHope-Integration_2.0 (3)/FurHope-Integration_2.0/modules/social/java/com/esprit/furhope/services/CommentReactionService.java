package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class CommentReactionService extends ConnectToDbService {

    public static class ReactionSnapshot {
        private final String reaction;
        private final int likesCount;
        private final int dislikesCount;

        public ReactionSnapshot(String reaction, int likesCount, int dislikesCount) {
            this.reaction = reaction;
            this.likesCount = likesCount;
            this.dislikesCount = dislikesCount;
        }

        public String getReaction() {
            return reaction;
        }

        public int getLikesCount() {
            return likesCount;
        }

        public int getDislikesCount() {
            return dislikesCount;
        }
    }

    private final NotificationServiceJdbc notifSvc = new NotificationServiceJdbc();

    public CommentReactionService() {
        super();
    }

    public ReactionSnapshot getSnapshot(long commentId, long userId) throws SQLException {
        String reaction = getReaction(commentId, userId);
        Counts counts = getCounts(commentId);
        return new ReactionSnapshot(reaction, counts.likesCount, counts.dislikesCount);
    }

    public ReactionSnapshot toggleReaction(long commentId, long userId, String desired) throws SQLException {
        String normalized = normalizeDesired(desired);
        String current = getReaction(commentId, userId);
        CommentOwnerInfo ownerInfo = findCommentOwner(commentId);
        if (ownerInfo == null) {
            throw new SQLException("Comment not found");
        }

        String finalReaction;
        con.setAutoCommit(false);
        try {
            if (current == null) {
                String sql = "INSERT INTO comment_reaction(comment_id, user_id, reaction) VALUES (?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, commentId);
                    ps.setLong(2, userId);
                    ps.setString(3, normalized);
                    ps.executeUpdate();
                }
                finalReaction = normalized;
            } else if (current.equalsIgnoreCase(normalized)) {
                String sql = "DELETE FROM comment_reaction WHERE comment_id = ? AND user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, commentId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                finalReaction = null;
            } else {
                String sql = "UPDATE comment_reaction SET reaction = ? WHERE comment_id = ? AND user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, normalized);
                    ps.setLong(2, commentId);
                    ps.setLong(3, userId);
                    ps.executeUpdate();
                }
                finalReaction = normalized;
            }

            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }

        if (finalReaction != null) {
            safeCreateReactionNotification(ownerInfo, (int) userId, commentId, finalReaction);
        }
        return getSnapshot(commentId, userId);
    }

    public String getReaction(long commentId, long userId) throws SQLException {
        String sql = "SELECT reaction FROM comment_reaction WHERE comment_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("reaction");
            }
        }
    }

    private String normalizeDesired(String desired) {
        if (desired == null) {
            throw new IllegalArgumentException("Reaction is required");
        }
        String normalized = desired.trim().toUpperCase(Locale.ROOT);
        if (!"LIKE".equals(normalized) && !"DISLIKE".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported reaction: " + desired);
        }
        return normalized;
    }

    private Counts getCounts(long commentId) throws SQLException {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN reaction = 'LIKE' THEN 1 ELSE 0 END), 0) AS likes_count, " +
                "COALESCE(SUM(CASE WHEN reaction = 'DISLIKE' THEN 1 ELSE 0 END), 0) AS dislikes_count " +
                "FROM comment_reaction WHERE comment_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new Counts(0, 0);
                }
                return new Counts(rs.getInt("likes_count"), rs.getInt("dislikes_count"));
            }
        }
    }

    private CommentOwnerInfo findCommentOwner(long commentId) throws SQLException {
        String sql = "SELECT author_id, post_id FROM comment WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CommentOwnerInfo(rs.getInt("author_id"), rs.getLong("post_id"));
            }
        }
    }

    private void safeCreateReactionNotification(CommentOwnerInfo ownerInfo, int actorId, long commentId, String reaction) {
        try {
            if ("LIKE".equalsIgnoreCase(reaction)) {
                notifSvc.createNotification(
                        ownerInfo.authorId,
                        actorId,
                        "COMMENT_LIKE",
                        ownerInfo.postId,
                        commentId,
                        "liked your comment"
                );
            } else {
                notifSvc.createNotification(
                        ownerInfo.authorId,
                        actorId,
                        "COMMENT_DISLIKE",
                        ownerInfo.postId,
                        commentId,
                        "disliked your comment"
                );
            }
        } catch (SQLException ignored) {
            // Keep reaction action successful even if notification insertion fails.
        }
    }

    private static class CommentOwnerInfo {
        private final int authorId;
        private final long postId;

        private CommentOwnerInfo(int authorId, long postId) {
            this.authorId = authorId;
            this.postId = postId;
        }
    }

    private static class Counts {
        private final int likesCount;
        private final int dislikesCount;

        private Counts(int likesCount, int dislikesCount) {
            this.likesCount = likesCount;
            this.dislikesCount = dislikesCount;
        }
    }
}

