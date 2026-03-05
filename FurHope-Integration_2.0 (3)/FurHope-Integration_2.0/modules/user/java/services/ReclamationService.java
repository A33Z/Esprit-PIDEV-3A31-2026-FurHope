package com.esprit.services;

import com.esprit.entities.Reclamation;
import com.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements ICrud<Reclamation> {

    private final Connection con;

    public ReclamationService() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reclamation reclamation) throws SQLException {
        String sql = "INSERT INTO reclamation (client_id, sujet, description, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reclamation.getClientId());
            ps.setString(2, reclamation.getSujet());
            ps.setString(3, reclamation.getDescription());
            ps.setString(4, reclamation.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reclamation.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reclamation> afficher() throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation ORDER BY id DESC";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                reclamations.add(mapReclamation(rs));
            }
        }
        return reclamations;
    }

    public List<Reclamation> afficherParClient(int clientId) throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE client_id = ? ORDER BY id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reclamations.add(mapReclamation(rs));
                }
            }
        }
        return reclamations;
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLException {
        String sql = "UPDATE reclamation SET sujet = ?, description = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reclamation.getSujet());
            ps.setString(2, reclamation.getDescription());
            ps.setString(3, reclamation.getStatus());
            ps.setInt(4, reclamation.getId());
            ps.executeUpdate();
        }
    }

    public boolean isOwnedByClient(int reclamationId, int clientId) throws SQLException {
        String sql = "SELECT 1 FROM reclamation WHERE id = ? AND client_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.setInt(2, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Reclamation mapReclamation(ResultSet rs) throws SQLException {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(rs.getInt("id"));
        reclamation.setClientId(rs.getInt("client_id"));
        reclamation.setSujet(rs.getString("sujet"));
        reclamation.setDescription(rs.getString("description"));
        reclamation.setStatus(rs.getString("status"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            reclamation.setCreatedAt(timestamp.toLocalDateTime());
        }
        return reclamation;
    }
}
