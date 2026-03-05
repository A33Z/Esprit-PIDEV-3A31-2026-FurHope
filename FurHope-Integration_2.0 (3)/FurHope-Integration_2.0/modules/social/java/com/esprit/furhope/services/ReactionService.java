package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReactionService extends ConnectToDbService {

    private final NotificationServiceJdbc notifSvc = new NotificationServiceJdbc();

    public ReactionService() {
        super();
    }

    public String getReaction(long postId, long userId) throws SQLException {
        String sql = "SELECT reaction FROM post_reaction WHERE post_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("reaction");
            }
        }
    }

    public String toggleReaction(long postId, long userId, String desired) throws SQLException {
        con.setAutoCommit(false);
        try {
            String current = getReaction(postId, userId);
            Integer postAuthorId = findPostAuthorId(postId);

            if (current == null) {
                String insertSql = "INSERT INTO post_reaction(post_id,user_id,reaction) VALUES (?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setLong(1, postId);
                    ps.setLong(2, userId);
                    ps.setString(3, desired);
                    ps.executeUpdate();
                }

                if ("LIKE".equalsIgnoreCase(desired)) {
                    String updateLike = "UPDATE post SET likes_count = likes_count + 1 WHERE id=?";
                    try (PreparedStatement ps = con.prepareStatement(updateLike)) {
                        ps.setLong(1, postId);
                        ps.executeUpdate();
                    }
                } else {
                    String updateDislike = "UPDATE post SET dislikes_count = dislikes_count + 1 WHERE id=?";
                    try (PreparedStatement ps = con.prepareStatement(updateDislike)) {
                        ps.setLong(1, postId);
                        ps.executeUpdate();
                    }
                }

                con.commit();
                createReactionNotification(postAuthorId, userId, postId, desired);
                return desired;
            }

            if (current.equalsIgnoreCase(desired)) {
                String deleteSql = "DELETE FROM post_reaction WHERE post_id=? AND user_id=?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setLong(1, postId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                if ("LIKE".equalsIgnoreCase(current)) {
                    String updateLike = "UPDATE post SET likes_count = GREATEST(likes_count - 1, 0) WHERE id=?";
                    try (PreparedStatement ps = con.prepareStatement(updateLike)) {
                        ps.setLong(1, postId);
                        ps.executeUpdate();
                    }
                } else {
                    String updateDislike = "UPDATE post SET dislikes_count = GREATEST(dislikes_count - 1, 0) WHERE id=?";
                    try (PreparedStatement ps = con.prepareStatement(updateDislike)) {
                        ps.setLong(1, postId);
                        ps.executeUpdate();
                    }
                }

                con.commit();
                return null;
            }

            String updateReaction = "UPDATE post_reaction SET reaction=? WHERE post_id=? AND user_id=?";
            try (PreparedStatement ps = con.prepareStatement(updateReaction)) {
                ps.setString(1, desired);
                ps.setLong(2, postId);
                ps.setLong(3, userId);
                ps.executeUpdate();
            }

            if ("LIKE".equalsIgnoreCase(current)) {
                String decLike = "UPDATE post SET likes_count = GREATEST(likes_count - 1, 0) WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(decLike)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }
            } else {
                String decDislike = "UPDATE post SET dislikes_count = GREATEST(dislikes_count - 1, 0) WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(decDislike)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }
            }

            if ("LIKE".equalsIgnoreCase(desired)) {
                String incLike = "UPDATE post SET likes_count = likes_count + 1 WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(incLike)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }
            } else {
                String incDislike = "UPDATE post SET dislikes_count = dislikes_count + 1 WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(incDislike)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }
            }

            con.commit();
            createReactionNotification(postAuthorId, userId, postId, desired);
            return desired;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private Integer findPostAuthorId(long postId) throws SQLException {
        String sql = "SELECT author_id FROM post WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt("author_id");
            }
        }
    }

    private void createReactionNotification(Integer postAuthorId, long userId, long postId, String desired) throws SQLException {
        if (postAuthorId == null) return;
        if ("LIKE".equalsIgnoreCase(desired)) {
            notifSvc.createNotification(postAuthorId, (int) userId, "POST_LIKE", postId, null, "Someone liked your post");
        } else if ("DISLIKE".equalsIgnoreCase(desired)) {
            notifSvc.createNotification(postAuthorId, (int) userId, "POST_DISLIKE", postId, null, "Someone disliked your post");
        }
    }
}
