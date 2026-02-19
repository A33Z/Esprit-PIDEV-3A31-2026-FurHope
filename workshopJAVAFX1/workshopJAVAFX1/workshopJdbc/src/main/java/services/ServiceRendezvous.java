package services;

import model.Rendezvous;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezvous implements IService<Rendezvous> {

    private final MyDatabase database;

    public ServiceRendezvous() {
        database = MyDatabase.getInstance();
    }

    @Override
    public void add(Rendezvous rendezvous) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "INSERT INTO rendezvous (status, description, client_id, vet_id, animal_id, disponibilite_id, app_date, app_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rendezvous.getStatus());
            ps.setString(2, rendezvous.getDescription());
            ps.setInt(3, rendezvous.getClient_id());
            ps.setInt(4, rendezvous.getVet_id());
            ps.setInt(5, rendezvous.getAnimal_id());
            ps.setInt(6, rendezvous.getDisponibilite_id());
            ps.setString(7, rendezvous.getApp_date());
            ps.setString(8, rendezvous.getApp_time());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rendezvous.setId_rdv(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Rendezvous rendezvous) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "UPDATE rendezvous SET status = ?, description = ?, client_id = ?, vet_id = ?, animal_id = ?, disponibilite_id = ?, app_date = ?, app_time = ? WHERE id_rdv = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rendezvous.getStatus());
            ps.setString(2, rendezvous.getDescription());
            ps.setInt(3, rendezvous.getClient_id());
            ps.setInt(4, rendezvous.getVet_id());
            ps.setInt(5, rendezvous.getAnimal_id());
            ps.setInt(6, rendezvous.getDisponibilite_id());
            ps.setString(7, rendezvous.getApp_date());
            ps.setString(8, rendezvous.getApp_time());
            ps.setInt(9, rendezvous.getId_rdv());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id_rdv) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "DELETE FROM rendezvous WHERE id_rdv = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id_rdv);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Rendezvous> read() throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM rendezvous ORDER BY id_rdv DESC";
        List<Rendezvous> rendezvousList = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Rendezvous r = new Rendezvous();
                r.setId_rdv(rs.getInt("id_rdv"));
                r.setStatus(rs.getString("status"));
                r.setDescription(rs.getString("description"));
                r.setClient_id(rs.getInt("client_id"));
                r.setVet_id(rs.getInt("vet_id"));
                r.setAnimal_id(rs.getInt("animal_id"));
                r.setDisponibilite_id(rs.getInt("disponibilite_id"));
                r.setApp_date(rs.getString("app_date"));
                r.setApp_time(rs.getString("app_time"));
                rendezvousList.add(r);
            }
        }

        return rendezvousList;
    }
}
