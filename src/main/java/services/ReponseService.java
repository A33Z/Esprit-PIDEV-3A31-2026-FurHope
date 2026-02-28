package com.esprit.services;

import com.esprit.entities.Reponse;
import com.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements ICrud<Reponse> {

    private final Connection con;

    public ReponseService() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reponse reponse) throws SQLException {
        String sql = "INSERT INTO reponse (reclamation_id, admin_id, message) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reponse.getReclamationId());
            ps.setInt(2, reponse.getAdminId());
            ps.setString(3, reponse.getMessage());
            ps.executeUpdate();
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
        String sql = "SELECT * FROM reponse WHERE reclamation_id = ? ORDER BY id DESC";
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

    private Reponse mapReponse(ResultSet rs) throws SQLException {
        Reponse reponse = new Reponse();
        reponse.setId(rs.getInt("id"));
        reponse.setReclamationId(rs.getInt("reclamation_id"));
        reponse.setAdminId(rs.getInt("admin_id"));
        reponse.setMessage(rs.getString("message"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            reponse.setCreatedAt(timestamp.toLocalDateTime());
        }
        return reponse;
    }
}
