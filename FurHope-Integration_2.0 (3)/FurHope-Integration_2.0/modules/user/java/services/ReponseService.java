package com.esprit.services;

import com.esprit.entities.Reponse;
import com.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements ICrud<Reponse> {

    private static volatile boolean schemaReady;
    private final Connection con;

    public ReponseService() {
        this.con = MyDataBase.getInstance().getConnection();
        ensureSchema();
    }

    @Override
    public void ajouter(Reponse reponse) throws SQLException {
        String senderType = normalizeSenderType(reponse.getSenderType());
        int senderId = reponse.getSenderId() == 0 ? reponse.getAdminId() : reponse.getSenderId();
        if (senderId == 0) {
            senderId = "AI".equals(senderType) ? -1 : 0;
        }

        String sql = "INSERT INTO reponse (reclamation_id, admin_id, sender_id, sender_type, message, rating) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reponse.getReclamationId());
            ps.setInt(2, reponse.getAdminId());
            ps.setInt(3, senderId);
            ps.setString(4, senderType);
            ps.setString(5, reponse.getMessage());
            if (reponse.getRating() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, reponse.getRating());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reponse.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reponse WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reponse> afficher() throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse ORDER BY id DESC";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                reponses.add(mapReponse(rs));
            }
        }
        return reponses;
    }

    public List<Reponse> afficherParReclamation(int reclamationId) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE reclamation_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reponses.add(mapReponse(rs));
                }
            }
        }
        return reponses;
    }

    @Override
    public void modifier(Reponse reponse) throws SQLException {
        String sql = "UPDATE reponse SET message = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reponse.getMessage());
            ps.setInt(2, reponse.getId());
            ps.executeUpdate();
        }
    }

    public void rateReponse(int reponseId, int rating) throws SQLException {
        int bounded = Math.max(1, Math.min(5, rating));
        String sql = "UPDATE reponse SET rating = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bounded);
            ps.setInt(2, reponseId);
            ps.executeUpdate();
        }
    }

    private Reponse mapReponse(ResultSet rs) throws SQLException {
        Reponse reponse = new Reponse();
        reponse.setId(rs.getInt("id"));
        reponse.setReclamationId(rs.getInt("reclamation_id"));
        reponse.setAdminId(rs.getInt("admin_id"));
        reponse.setSenderId(getIntOrDefault(rs, "sender_id", reponse.getAdminId()));
        reponse.setSenderType(getStringOrDefault(rs, "sender_type", inferLegacySenderType(reponse.getAdminId())));
        reponse.setMessage(rs.getString("message"));
        if (hasColumn(rs, "rating")) {
            int value = rs.getInt("rating");
            reponse.setRating(rs.wasNull() ? null : value);
        }
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            reponse.setCreatedAt(timestamp.toLocalDateTime());
        }
        return reponse;
    }

    private String inferLegacySenderType(int adminId) {
        if (adminId <= 0) {
            return "AI";
        }
        return "ADMIN";
    }

    private int getIntOrDefault(ResultSet rs, String column, int fallback) throws SQLException {
        if (!hasColumn(rs, column)) {
            return fallback;
        }
        int value = rs.getInt(column);
        return rs.wasNull() ? fallback : value;
    }

    private String getStringOrDefault(ResultSet rs, String column, String fallback) throws SQLException {
        if (!hasColumn(rs, column)) {
            return fallback;
        }
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private boolean hasColumn(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private String normalizeSenderType(String senderType) {
        if (senderType == null || senderType.isBlank()) {
            return "ADMIN";
        }
        String normalized = senderType.trim().toUpperCase();
        if ("CLIENT".equals(normalized) || "AI".equals(normalized) || "ADMIN".equals(normalized)) {
            return normalized;
        }
        return "ADMIN";
    }

    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (ReponseService.class) {
            if (schemaReady) {
                return;
            }
            try (Statement st = con.createStatement()) {
                if (!columnExists("reponse", "sender_id")) {
                    st.executeUpdate("ALTER TABLE reponse ADD COLUMN sender_id INT NULL");
                }
                if (!columnExists("reponse", "sender_type")) {
                    st.executeUpdate("ALTER TABLE reponse ADD COLUMN sender_type VARCHAR(16) NULL");
                }
                if (!columnExists("reponse", "rating")) {
                    st.executeUpdate("ALTER TABLE reponse ADD COLUMN rating INT NULL");
                }

                st.executeUpdate(
                        """
                        UPDATE reponse
                        SET sender_id = COALESCE(sender_id, admin_id, 0),
                            sender_type = COALESCE(NULLIF(TRIM(sender_type), ''), CASE WHEN admin_id > 0 THEN 'ADMIN' ELSE 'AI' END)
                        """
                );
                schemaReady = true;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to prepare response chat schema.", e);
            }
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet rs = metaData.getColumns(con.getCatalog(), null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getColumns(con.getCatalog(), null, table.toUpperCase(), column.toUpperCase())) {
            return rs.next();
        }
    }
}
