package com.esprit.furhope.services;

import com.esprit.furhope.entities.comment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class CommentServiceJdbc extends ConnectToDbService implements C_R_U_D<comment> {

    private static final String AUTHOR_NAME_SQL =
            "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''), " +
                    "u.email, CONCAT('User ', c.author_id)) AS author_name ";
    private static final String AUTHOR_PROFILE_IMAGE_SQL =
            "u.profile_image_path AS author_profile_image_path ";

    private final NotificationServiceJdbc notificationService = new NotificationServiceJdbc();

    public CommentServiceJdbc() {
        super();
    }

    public void addComment(comment c) throws SQLException {
        String sql =
                "INSERT INTO comment (post_id, author_id, parent_comment_id, body, status) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getPostId());
            ps.setInt(2, c.getAuthorId());

            if (c.getParentCommentId() == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, c.getParentCommentId());
            }

            ps.setString(4, c.getBody());
            ps.setString(5, c.getStatus() == null ? "ACTIVE" : c.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setId(keys.getLong(1));
                }
            }
        }

        incrementPostCommentsCount(c.getPostId(), 1);
        createCommentNotifications(c);
    }

    public void addReply(long parentCommentId, comment reply) throws SQLException {
        reply.setParentCommentId(parentCommentId);
        addComment(reply);
    }

    public void updateComment(comment c) throws SQLException {
        if (!updateComment(c, c.getAuthorId())) {
            throw new SQLException("Not allowed to edit this comment");
        }
    }

    public boolean updateComment(comment c, int actorId) throws SQLException {
        if (!canManageComment(c.getId(), actorId)) {
            return false;
        }
        String sql = "UPDATE comment SET body=?, status=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getBody());
            ps.setString(2, c.getStatus() == null ? "ACTIVE" : c.getStatus());
            ps.setLong(3, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteComment(long commentId) throws SQLException {
        return deleteCommentInternal(commentId);
    }

    public boolean deleteComment(long commentId, int actorId) throws SQLException {
        if (!canManageComment(commentId, actorId)) {
            return false;
        }
        return deleteCommentInternal(commentId);
    }

    private boolean deleteCommentInternal(long commentId) throws SQLException {
        con.setAutoCommit(false);
        try {
            Long postId = findPostIdByCommentId(commentId);
            if (postId == null) {
                con.rollback();
                return false;
            }

            List<Long> commentTreeIds = collectCommentTreeIds(commentId);
            if (commentTreeIds.isEmpty()) {
                commentTreeIds = Collections.singletonList(commentId);
            }

            deleteNotificationsByCommentIds(commentTreeIds);
            int deleted = deleteCommentsByIds(commentTreeIds);

            if (deleted > 0) {
                incrementPostCommentsCount(postId, -deleted);
            }

            con.commit();
            return deleted > 0;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public List<comment> listComments(long postId) throws SQLException {
        String sql = "SELECT c.*, " +
                AUTHOR_NAME_SQL + ", " +
                AUTHOR_PROFILE_IMAGE_SQL +
                "FROM comment c " +
                "LEFT JOIN `user` u ON u.id = c.author_id " +
                "WHERE c.post_id=? AND c.status = ? " +
                "ORDER BY c.created_at ASC";
        List<comment> comments = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(map(rs));
                }
            }
        }

        return comments;
    }

    public List<comment> listReplies(long parentCommentId) throws SQLException {
        String sql = "SELECT c.*, " +
                AUTHOR_NAME_SQL + ", " +
                AUTHOR_PROFILE_IMAGE_SQL +
                "FROM comment c " +
                "LEFT JOIN `user` u ON u.id = c.author_id " +
                "WHERE c.parent_comment_id=? AND c.status = ? " +
                "ORDER BY c.created_at ASC";
        List<comment> replies = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, parentCommentId);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    replies.add(map(rs));
                }
            }
        }
        return replies;
    }

    public void deleteByPostId(long postId) throws SQLException {
        con.setAutoCommit(false);
        try {
            List<Long> commentIds = findCommentIdsByPostId(postId);
            if (!commentIds.isEmpty()) {
                deleteNotificationsByCommentIds(commentIds);
                deleteCommentsByIds(commentIds);
            }
            resetPostCommentsCount(postId);
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    @Override
    public void ajouter(comment c) throws SQLException {
        addComment(c);
    }

    @Override
    public void supprimer(long id) throws SQLException {
        deleteComment(id);
    }

    @Override
    public List<comment> afficher() throws SQLException {
        String sql = "SELECT c.*, " +
                AUTHOR_NAME_SQL + ", " +
                AUTHOR_PROFILE_IMAGE_SQL +
                "FROM comment c " +
                "LEFT JOIN `user` u ON u.id = c.author_id " +
                "ORDER BY c.created_at DESC";
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
        updateComment(c);
    }

    public List<comment> findByPostId(long postId) throws SQLException {
        return listComments(postId);
    }

    private comment map(ResultSet rs) throws SQLException {
        comment c = new comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setAuthorId(rs.getInt("author_id"));
        c.setAuthorName(rs.getString("author_name"));
        c.setAuthorProfileImagePath(rs.getString("author_profile_image_path"));

        long parent = rs.getLong("parent_comment_id");
        c.setParentCommentId(rs.wasNull() ? null : parent);

        c.setBody(rs.getString("body"));
        c.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        c.setCreatedAt(createdAt);
        return c;
    }

    private void createCommentNotifications(comment c) throws SQLException {
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

    private void incrementPostCommentsCount(long postId, int delta) throws SQLException {
        String sql = "UPDATE post SET comments_count = GREATEST(comments_count + ?, 0) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    private void resetPostCommentsCount(long postId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE post SET comments_count = 0 WHERE id = ?")) {
            ps.setLong(1, postId);
            ps.executeUpdate();
        }
    }

    private Long findPostIdByCommentId(long commentId) throws SQLException {
        String sql = "SELECT post_id FROM comment WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getLong("post_id");
            }
        }
    }

    private boolean canManageComment(long commentId, int actorId) throws SQLException {
        if (actorId <= 0 || commentId <= 0) {
            return false;
        }

        String sql = "SELECT c.author_id AS comment_author_id, p.author_id AS post_author_id " +
                "FROM comment c " +
                "JOIN post p ON p.id = c.post_id " +
                "WHERE c.id = ? " +
                "LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                int commentAuthorId = rs.getInt("comment_author_id");
                int postAuthorId = rs.getInt("post_author_id");
                return actorId == commentAuthorId || actorId == postAuthorId;
            }
        }
    }

    private Integer findPostAuthorId(long postId) throws SQLException {
        String sql = "SELECT author_id FROM post WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt("author_id");
            }
        }
    }

    private ParentCommentInfo findCommentAuthorId(long parentId) throws SQLException {
        String sql = "SELECT author_id, post_id FROM comment WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ParentCommentInfo(rs.getInt("author_id"), rs.getLong("post_id"));
            }
        }
    }

    private List<Long> collectCommentTreeIds(long rootCommentId) throws SQLException {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        ids.add(rootCommentId);
        queue.add(rootCommentId);

        while (!queue.isEmpty()) {
            long parentId = queue.removeFirst();
            String sql = "SELECT id FROM comment WHERE parent_comment_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, parentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long childId = rs.getLong("id");
                        if (ids.add(childId)) {
                            queue.add(childId);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private int deleteCommentsByIds(List<Long> commentIds) throws SQLException {
        if (commentIds == null || commentIds.isEmpty()) {
            return 0;
        }

        int deleted = 0;
        List<Long> reversed = new ArrayList<>(commentIds);
        Collections.reverse(reversed);
        for (Long id : reversed) {
            if (id == null) {
                continue;
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM comment WHERE id = ?")) {
                ps.setLong(1, id);
                deleted += ps.executeUpdate();
            }
        }
        return deleted;
    }

    private List<Long> findCommentIdsByPostId(long postId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM comment WHERE post_id = ?")) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
        }
        return ids;
    }

    private void deleteNotificationsByCommentIds(List<Long> commentIds) throws SQLException {
        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(commentIds.size(), "?"));
        String sql = "DELETE FROM notification WHERE comment_id IN (" + placeholders + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < commentIds.size(); i++) {
                ps.setLong(i + 1, commentIds.get(i));
            }
            ps.executeUpdate();
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
