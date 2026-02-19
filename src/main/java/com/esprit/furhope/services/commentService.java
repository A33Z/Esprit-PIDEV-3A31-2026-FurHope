package com.esprit.furhope.services;

import com.esprit.furhope.entities.comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class commentService extends ConnectToDbService implements C_R_U_D<comment> {

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
    }

    @Override
    public void supprimer(long id) throws SQLException {
        Long postId = findPostIdByCommentId(id);

        String sql = "DELETE FROM comment WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }

        if (postId != null) {
            incrementPostCommentsCount(postId, -1);
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
        String sql = "SELECT * FROM comment WHERE post_id=? ORDER BY created_at ASC";
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

    private comment map(ResultSet rs) throws SQLException {
        comment c = new comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setAuthorId(rs.getInt("author_id"));

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
}
