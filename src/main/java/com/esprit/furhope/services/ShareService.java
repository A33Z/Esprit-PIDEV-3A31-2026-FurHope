package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShareService extends ConnectToDbService {

    public ShareService() {
        super();
    }

    public boolean hasShared(long postId, long userId) throws SQLException {
        String sql = "SELECT 1 FROM post_share WHERE post_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean toggleShare(long postId, long userId) throws SQLException {
        con.setAutoCommit(false);
        try {
            boolean alreadyShared = hasShared(postId, userId);
            if (!alreadyShared) {
                String insertSql = "INSERT INTO post_share(post_id,user_id) VALUES (?,?)";
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setLong(1, postId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                String updateSql = "UPDATE post SET shares_count = shares_count + 1 WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }

                con.commit();
                return true;
            } else {
                String deleteSql = "DELETE FROM post_share WHERE post_id=? AND user_id=?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setLong(1, postId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                String updateSql = "UPDATE post SET shares_count = GREATEST(shares_count - 1, 0) WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setLong(1, postId);
                    ps.executeUpdate();
                }

                con.commit();
                return false;
            }
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }
}
