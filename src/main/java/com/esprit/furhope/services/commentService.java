package com.esprit.furhope.services;

import com.esprit.furhope.entities.comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class commentService extends ConnectToDbService implements C_R_U_D<comment> {

    private final NotificationService notificationService = new NotificationService();

    public commentService() {
        super();
    }

    @Override
    public void ajouter(comment c) throws SQLException {
        String sql =
                "INSERT INTO comment (post_id, author_id, parent_comment_id, body, status) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getPostId());
            ps.setInt(2, c.getAuthorId());

            if (c.getParentCommentId() == null) ps.setNull(3, Types.BIGINT);
            else ps.setLong(3, c.getParentCommentId());

            ps.setString(4, c.getBody());
            ps.setString(5, c.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getLong(1));
            }
        }

        // Optional (recommended): keep post.comments_count consistent
        incrementPostCommentsCount(c.getPostId(), 1);

        Integer postAuthorId = findPostAuthorId(c.getPostId());
        if (postAuthorId != null) {
            Long commentId = c.getId() > 0 ? c.getId() : null;
            notificationService.createNotification(
                    postAuthorId,
                    c.getAuthorId(),
                    "POST_COMMENT",
                    c.getPostId(),
                    commentId,
                    "New comment on your post"
            );
        }

        if (c.getParentCommentId() != null) {
            ParentCommentInfo parent = findCommentAuthorId(c.getParentCommentId());
            if (parent != null) {
                Long commentId = c.getId() > 0 ? c.getId() : null;
                notificationService.createNotification(
                        parent.authorId,
                        c.getAuthorId(),
                        "COMMENT_REPLY",
                        parent.postId,
                        commentId,
                        "replied to your comment"
                );
            }
        }
    }

    @Override
    public void supprimer(long id) throws SQLException {
        con.setAutoCommit(false);
        try {
            Long postId = findPostIdByCommentId(id);
            int totalDeleted = 1;

            String countSql = "SELECT 1 + (SELECT COUNT(*) FROM comment WHERE parent_comment_id = ?) AS total";
            try (PreparedStatement ps = con.prepareStatement(countSql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalDeleted = rs.getInt("total");
                    }
                }
            }

            String sql = "DELETE FROM comment WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            if (postId != null) {
                String updateSql = "UPDATE post SET comments_count = GREATEST(comments_count - ?, 0) WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, totalDeleted);
                    ps.setLong(2, postId);
                    ps.executeUpdate();
                }
            }

            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    @Override
    public List<comment> afficher() throws SQLException {
        String sql = "SELECT * FROM comment ORDER BY created_at DESC";
        List<comment> comments = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comments.add(map(rs));
            }
        }

        return comments;
    }

    @Override
    public void modifier(comment c) throws SQLException {
        String sql = "UPDATE comment SET body=?, status=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getBody());
            ps.setString(2, c.getStatus());
            ps.setLong(3, c.getId());
            ps.executeUpdate();
        }
    }

    public List<comment> findByPostId(long postId) throws SQLException {
        String sql = "SELECT c.*, u.name AS author_name " +
                "FROM comment c " +
                "LEFT JOIN `user` u ON u.id_user = c.author_id " +
                "WHERE c.post_id=? " +
                "ORDER BY c.created_at ASC";
        List<comment> comments = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(map(rs));
                }
            }
        }

        return comments;
    }

    public void deleteByPostId(long postId) throws SQLException {
        String sql = "DELETE FROM comment WHERE post_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.executeUpdate();
        }
    }

    private comment map(ResultSet rs) throws SQLException {
        comment c = new comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setAuthorId(rs.getInt("author_id"));
        try {
            c.setAuthorName(rs.getString("author_name"));
        } catch (SQLException ignored) {
            c.setAuthorName(null);
        }

        long parent = rs.getLong("parent_comment_id");
        c.setParentCommentId(rs.wasNull() ? null : parent);

        c.setBody(rs.getString("body"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }

    private void incrementPostCommentsCount(long postId, int delta) throws SQLException {
        String sql = "UPDATE post SET comments_count = GREATEST(comments_count + ?, 0) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    private Long findPostIdByCommentId(long commentId) throws SQLException {
        String sql = "SELECT post_id FROM comment WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getLong("post_id");
            }
        }
    }

    private Integer findPostAuthorId(long postId) throws SQLException {
        String sql = "SELECT author_id FROM post WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt("author_id");
            }
        }
    }

    private ParentCommentInfo findCommentAuthorId(long parentId) throws SQLException {
        String sql = "SELECT author_id, post_id FROM comment WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ParentCommentInfo(rs.getInt("author_id"), rs.getLong("post_id"));
            }
        }
    }

    private static class ParentCommentInfo {
        private final int authorId;
        private final long postId;

        private ParentCommentInfo(int authorId, long postId) {
            this.authorId = authorId;
            this.postId = postId;
        }
    }
}
