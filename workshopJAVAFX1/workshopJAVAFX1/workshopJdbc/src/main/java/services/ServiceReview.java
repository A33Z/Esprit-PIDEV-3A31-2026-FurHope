package services;

import model.Review;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReview {

    private final MyDatabase database = MyDatabase.getInstance();

    public void add(Review review) throws SQLException {
        String sql = "INSERT INTO review (client_id, vet_id, rdv_id, rating, commentaire) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, review.getClientId());
            ps.setInt(2, review.getVetId());
            ps.setInt(3, review.getRdvId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getCommentaire());
            ps.executeUpdate();
        }
    }

    public double getAverageRating(int vetId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM review WHERE vet_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public int getNombreAvis(int vetId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review WHERE vet_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ✅ RDV confirmés non encore notés par ce client
    public List<int[]> getRdvConfirmesNonNotes(int clientId) throws SQLException {
        String sql = "SELECT r.id_rdv, r.vet_id, u.first_name, u.last_name " +
                "FROM rendezvous r " +
                "JOIN user u ON u.id = r.vet_id " +
                "LEFT JOIN review rv ON rv.rdv_id = r.id_rdv " +
                "WHERE r.client_id = ? AND r.status = 'TERMINE' " +
                "AND rv.id IS NULL";
        List<int[]> result = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new int[]{
                        rs.getInt("id_rdv"),
                        rs.getInt("vet_id")
                });
            }
        }
        return result;
    }

    // ✅ Vérifier si déjà noté
    public boolean dejaNote(int rdvId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review WHERE rdv_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, rdvId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }
}