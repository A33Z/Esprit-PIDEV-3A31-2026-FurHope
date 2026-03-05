package com.esprit.furhope.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportService extends ConnectToDbService {

    private final NotificationServiceJdbc notifSvc = new NotificationServiceJdbc();

    public ReportService() {
        super();
    }

    public boolean hasReported(long postId, long reporterUserId) throws SQLException {
        String sql = "SELECT 1 FROM post_report WHERE post_id=? AND reporter_user_id=? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, reporterUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean reportPost(long postId, long reporterUserId, String reason) throws SQLException {
        if (hasReported(postId, reporterUserId)) {
            return false;
        }

        String sql = "INSERT INTO post_report(post_id, reporter_user_id, reason) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, reporterUserId);
            if (reason == null || reason.isBlank()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, reason.trim());
            }
            ps.executeUpdate();
            safeReportNotification(postId, reporterUserId);
            return true;
        } catch (SQLException e) {
            String state = e.getSQLState();
            if (state != null && state.startsWith("23")) {
                return false;
            }
            throw e;
        }
    }

    private void safeReportNotification(long postId, long actorId) {
        try {
            Integer postAuthorId = findPostAuthorId(postId);
            if (postAuthorId == null) {
                return;
            }
            notifSvc.createNotification(
                    postAuthorId,
                    (int) actorId,
                    "POST_REPORT",
                    postId,
                    null,
                    "reported your post"
            );
        } catch (SQLException ignored) {
            // Keep report flow successful even if notification insertion fails.
        }
    }

    private Integer findPostAuthorId(long postId) throws SQLException {
        String sql = "SELECT author_id FROM post WHERE id = ? LIMIT 1";
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
}
