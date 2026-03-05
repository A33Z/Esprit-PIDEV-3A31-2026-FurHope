package com.esprit.furhope.services;

import com.esprit.furhope.entities.post;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PostServiceJdbc extends ConnectToDbService implements C_R_U_D<post> {

    private static final String AUTHOR_NAME_SQL =
            "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''), " +
                    "u.email, CONCAT('User ', p.author_id)) AS author_name ";
    private static final String AUTHOR_PROFILE_IMAGE_SQL =
            "u.profile_image_path AS author_profile_image_path ";

    public PostServiceJdbc() {
        super();
    }

    public void createPost(post p) throws SQLException {
        String sql =
                "INSERT INTO post (author_id, caption, media_type, media_path, thumbnail_path, duration_seconds, visibility, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getAuthorId());
            ps.setString(2, p.getCaption());
            ps.setString(3, p.getMediaType());
            if (p.getMediaPath() == null || p.getMediaPath().isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, p.getMediaPath());
            }
            if (p.getThumbnailPath() == null || p.getThumbnailPath().isBlank()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, p.getThumbnailPath());
            }

            if (p.getDurationSeconds() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, p.getDurationSeconds());
            }

            ps.setString(7, p.getVisibility());
            ps.setString(8, p.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getLong(1));
                }
            }
        }
    }

    public void updatePost(post p) throws SQLException {
        String sql =
                "UPDATE post " +
                        "SET caption=?, media_type=?, media_path=?, thumbnail_path=?, duration_seconds=?, visibility=?, status=? " +
                        "WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getCaption());
            ps.setString(2, p.getMediaType());

            if (p.getMediaPath() == null || p.getMediaPath().isBlank()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, p.getMediaPath());
            }

            if (p.getThumbnailPath() == null || p.getThumbnailPath().isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, p.getThumbnailPath());
            }

            if (p.getDurationSeconds() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, p.getDurationSeconds());
            }

            ps.setString(6, p.getVisibility());
            ps.setString(7, p.getStatus());
            ps.setLong(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void deletePost(long id) throws SQLException {
        String sql = "DELETE FROM post WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<post> getFeedPosts(long viewerId) throws SQLException {
        String sql = "SELECT p.*, " +
                AUTHOR_NAME_SQL + ", " +
                AUTHOR_PROFILE_IMAGE_SQL +
                "FROM post p " +
                "LEFT JOIN `user` u ON u.id = p.author_id " +
                "WHERE p.status = ? " +
                "AND (" +
                "p.visibility = ? " +
                "OR (p.visibility = ? AND p.author_id = ?) " +
                "OR (p.visibility = ? AND (" +
                "p.author_id = ? " +
                "OR EXISTS (" +
                "SELECT 1 FROM friendship f " +
                "WHERE f.user1_id = LEAST(p.author_id, ?) " +
                "AND f.user2_id = GREATEST(p.author_id, ?)" +
                ")" +
                "))" +
                ") " +
                "ORDER BY p.created_at DESC";

        List<post> posts = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            ps.setString(2, "PUBLIC");
            ps.setString(3, "PRIVATE");
            ps.setLong(4, viewerId);
            ps.setString(5, "FRIENDS");
            ps.setLong(6, viewerId);
            ps.setLong(7, viewerId);
            ps.setLong(8, viewerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(map(rs));
                }
            }
        }
        return posts;
    }

    public post getPostById(long id) throws SQLException {
        String sql = "SELECT p.*, " +
                AUTHOR_NAME_SQL + ", " +
                AUTHOR_PROFILE_IMAGE_SQL +
                "FROM post p " +
                "LEFT JOIN `user` u ON u.id = p.author_id " +
                "WHERE p.id = ? AND p.status = ? " +
                "LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return map(rs);
            }
        }
    }

    public void incrementLike(long postId) throws SQLException {
        incrementLikes(postId, 1);
    }

    public void decrementLike(long postId) throws SQLException {
        incrementLikes(postId, -1);
    }

    public void incrementDislike(long postId) throws SQLException {
        incrementDislikes(postId, 1);
    }

    public void decrementDislike(long postId) throws SQLException {
        incrementDislikes(postId, -1);
    }

    public void incrementShare(long postId) throws SQLException {
        incrementShares(postId, 1);
    }

    public void decrementShare(long postId) throws SQLException {
        incrementShares(postId, -1);
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

    public Long getFirstPostId() throws SQLException {
        String sql = "SELECT id FROM post ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("id");
            }
            return null;
        }
    }

    @Override
    public void ajouter(post p) throws SQLException {
        createPost(p);
    }

    @Override
    public void supprimer(long id) throws SQLException {
        deletePost(id);
    }

    @Override
    public List<post> afficher() throws SQLException {
        return getFeedPosts(-1);
    }

    public List<post> afficher(long currentUserId) throws SQLException {
        return getFeedPosts(currentUserId);
    }

    @Override
    public void modifier(post p) throws SQLException {
        updatePost(p);
    }

    public post findById(long id) throws SQLException {
        return getPostById(id);
    }

    private post map(ResultSet rs) throws SQLException {
        post p = new post();
        p.setId(rs.getLong("id"));
        p.setAuthorId(rs.getInt("author_id"));
        p.setAuthorName(rs.getString("author_name"));
        p.setAuthorProfileImagePath(rs.getString("author_profile_image_path"));
        p.setCaption(rs.getString("caption"));
        p.setMediaType(rs.getString("media_type"));
        p.setMediaPath(rs.getString("media_path"));
        p.setThumbnailPath(rs.getString("thumbnail_path"));

        int duration = rs.getInt("duration_seconds");
        p.setDurationSeconds(rs.wasNull() ? null : duration);

        p.setLikesCount(rs.getInt("likes_count"));
        p.setDislikesCount(rs.getInt("dislikes_count"));
        p.setSharesCount(rs.getInt("shares_count"));
        p.setCommentsCount(rs.getInt("comments_count"));
        p.setVisibility(rs.getString("visibility"));
        p.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setCreatedAt(createdAt);
        p.setUpdatedAt(updatedAt);
        return p;
    }
}
