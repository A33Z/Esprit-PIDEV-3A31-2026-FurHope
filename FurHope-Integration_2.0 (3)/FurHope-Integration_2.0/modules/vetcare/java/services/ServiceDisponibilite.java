package services;

import model.Disponibilite;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDisponibilite implements IService<Disponibilite> {

    private final MyDatabase database;

    public ServiceDisponibilite() {
        database = MyDatabase.getInstance();
    }

    @Override
    public void add(Disponibilite disponibilite) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "INSERT INTO disponibilite (id, starttime, endtime, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, disponibilite.getId());
            ps.setTimestamp(2, Timestamp.valueOf(disponibilite.getStarttime()));
            ps.setTimestamp(3, Timestamp.valueOf(disponibilite.getEndtime()));
            ps.setString(4, disponibilite.getStatut().name().toLowerCase());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) disponibilite.setId_disponibilite(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Disponibilite disponibilite) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "UPDATE disponibilite SET id = ?, starttime = ?, endtime = ?, statut = ? WHERE id_disponibilite = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, disponibilite.getId());
            ps.setTimestamp(2, Timestamp.valueOf(disponibilite.getStarttime()));
            ps.setTimestamp(3, Timestamp.valueOf(disponibilite.getEndtime()));
            ps.setString(4, disponibilite.getStatut().name().toLowerCase());
            ps.setInt(5, disponibilite.getId_disponibilite());
            ps.executeUpdate();
        }
    }

    // ✅ Méthode ajoutée — change le statut automatiquement
    public void updateStatut(int dispoId, String statut) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "UPDATE disponibilite SET statut = ? WHERE id_disponibilite = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, dispoId);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id_disponibilite) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "DELETE FROM disponibilite WHERE id_disponibilite = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id_disponibilite);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Disponibilite> read() throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM disponibilite ORDER BY id_disponibilite DESC";
        List<Disponibilite> disponibilites = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) disponibilites.add(mapRow(rs));
        }
        return disponibilites;
    }

    public List<Disponibilite> readValables() throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM disponibilite WHERE statut = 'valable' ORDER BY starttime";
        List<Disponibilite> disponibilites = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) disponibilites.add(mapRow(rs));
        }
        return disponibilites;
    }

    public List<Disponibilite> readByVetId(int vetId) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM disponibilite WHERE id = ? ORDER BY starttime";
        List<Disponibilite> disponibilites = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) disponibilites.add(mapRow(rs));
            }
        }
        return disponibilites;
    }

    public Disponibilite findById(int id) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM disponibilite WHERE id_disponibilite = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Disponibilite mapRow(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId_disponibilite(rs.getInt("id_disponibilite"));
        d.setId(rs.getInt("id"));
        if (rs.getTimestamp("starttime") != null)
            d.setStarttime(rs.getTimestamp("starttime").toLocalDateTime());
        if (rs.getTimestamp("endtime") != null)
            d.setEndtime(rs.getTimestamp("endtime").toLocalDateTime());
        d.setStatut(parseStatut(rs.getString("statut")));
        return d;
    }

    private Disponibilite.Statut parseStatut(String raw) {
        if (raw == null) return Disponibilite.Statut.NONVALABLE;
        return Disponibilite.Statut.valueOf(raw.trim().toUpperCase());
    }
}