package com.esprit.furhope.services;

import com.esprit.furhope.entities.post;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class postService extends ConnectToDbService implements C_R_U_D<post> {

    public postService() {
        super();
    }

    @Override
    public void ajouter(post p) throws SQLException {
        String sql =
                "INSERT INTO post (author_id, caption, media_type, media_path, thumbnail_path, duration_seconds, visibility, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getAuthorId());
            ps.setString(2, p.getCaption());
            ps.setString(3, p.getMediaType());
            if (p.getMediaPath() == null || p.getMediaPath().isEmpty())
                ps.setNull(4, Types.VARCHAR);
            else
                ps.setString(4, p.getMediaPath());
            if (p.getThumbnailPath() == null || p.getThumbnailPath().isEmpty())
                ps.setNull(5, Types.VARCHAR);
            else
                ps.setString(5, p.getThumbnailPath());

            if (p.getDurationSeconds() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, p.getDurationSeconds());

            ps.setString(7, p.getVisibility());
            ps.setString(8, p.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getLong(1));
            }
        }
    }

    @Override
    public void supprimer(long id) throws SQLException {
        String sql = "DELETE FROM post WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<post> afficher() throws SQLException {
        String sql = "SELECT * FROM post ORDER BY created_at DESC";
        List<post> posts = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(map(rs));
            }
        }

        return posts;
    }

    @Override
    public void modifier(post p) throws SQLException {
        String sql =
                "UPDATE post " +
                        "SET caption=?, media_type=?, media_path=?, thumbnail_path=?, duration_seconds=?, visibility=?, status=? " +
                        "WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getCaption());
            ps.setString(2, p.getMediaType());
            ps.setString(3, p.getMediaPath());
            ps.setString(4, p.getThumbnailPath());

            if (p.getDurationSeconds() == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, p.getDurationSeconds());

            ps.setString(6, p.getVisibility());
            ps.setString(7, p.getStatus());
            ps.setLong(8, p.getId());

            ps.executeUpdate();
        }
    }

    public post findById(long id) throws SQLException {
        String sql = "SELECT * FROM post WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public void incrementLikes(long postId, int delta) throws SQLException {
        String sql = "UPDATE post SET likes_count = GREATEST(likes_count + ?, 0) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    public void incrementDislikes(long postId, int delta) throws SQLException {
        String sql = "UPDATE post SET dislikes_count = GREATEST(dislikes_count + ?, 0) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    public void incrementShares(long postId, int delta) throws SQLException {
        String sql = "UPDATE post SET shares_count = GREATEST(shares_count + ?, 0) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    /** Returns the id of the first post (by id), or null if there are no posts. */
    public Long getFirstPostId() throws SQLException {
        String sql = "SELECT id FROM post ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return rs.getLong("id");
        }
    }

    private post map(ResultSet rs) throws SQLException {
        post p = new post();
        p.setId(rs.getLong("id"));
        p.setAuthorId(rs.getInt("author_id"));
        p.setCaption(rs.getString("caption"));

        p.setMediaType(rs.getString("media_type"));
        p.setMediaPath(rs.getString("media_path"));
        p.setThumbnailPath(rs.getString("thumbnail_path"));

        int dur = rs.getInt("duration_seconds");
        p.setDurationSeconds(rs.wasNull() ? null : dur);

        p.setLikesCount(rs.getInt("likes_count"));
        p.setDislikesCount(rs.getInt("dislikes_count"));
        p.setSharesCount(rs.getInt("shares_count"));
        p.setCommentsCount(rs.getInt("comments_count"));

        p.setVisibility(rs.getString("visibility"));
        p.setStatus(rs.getString("status"));

        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }
}
