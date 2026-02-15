package services;

import entities.Reservation;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private Connection connection;

    public ReservationService() {
        try {
            connection = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // CREATE
    public boolean addReservation(Reservation r) {

        // Basic date validation
        if (r.getEndDate().before(r.getStartDate())) {
            System.out.println("End date must be after start date.");
            return false;
        }

        String sql = "INSERT INTO reservation (client_id, animal_id, hotel_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, r.getClientId());
            ps.setInt(2, r.getAnimalId());
            ps.setInt(3, r.getHotelId());
            ps.setDate(4, r.getStartDate());
            ps.setDate(5, r.getEndDate());
            ps.setString(6, r.getStatus());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reservation r = new Reservation(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("animal_id"),
                        rs.getInt("hotel_id"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getString("status")
                );
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // READ BY CLIENT ID
    public List<Reservation> getReservationsByClientId(int clientId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE client_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation(
                            rs.getInt("id"),
                            rs.getInt("client_id"),
                            rs.getInt("animal_id"),
                            rs.getInt("hotel_id"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getString("status")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // READ BY ID
    public Reservation getReservationById(int id) {
        String sql = "SELECT * FROM reservation WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Reservation(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("animal_id"),
                        rs.getInt("hotel_id"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getString("status")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE
    public boolean updateReservation(Reservation r) {

        if (r.getEndDate().before(r.getStartDate())) {
            System.out.println("Invalid dates.");
            return false;
        }

        String sql = "UPDATE reservation SET client_id=?, animal_id=?, hotel_id=?, start_date=?, end_date=?, status=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, r.getClientId());
            ps.setInt(2, r.getAnimalId());
            ps.setInt(3, r.getHotelId());
            ps.setDate(4, r.getStartDate());
            ps.setDate(5, r.getEndDate());
            ps.setString(6, r.getStatus());
            ps.setInt(7, r.getId());

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // UPDATE (OWNERSHIP-SAFE)
    public boolean updateReservationOwnedByClient(Reservation r, int clientId) {

        if (r.getEndDate().before(r.getStartDate())) {
            System.out.println("Invalid dates.");
            return false;
        }

        String sql = "UPDATE reservation SET client_id=?, animal_id=?, hotel_id=?, start_date=?, end_date=?, status=? " +
                "WHERE id=? AND client_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, clientId);
            ps.setInt(2, r.getAnimalId());
            ps.setInt(3, r.getHotelId());
            ps.setDate(4, r.getStartDate());
            ps.setDate(5, r.getEndDate());
            ps.setString(6, r.getStatus());
            ps.setInt(7, r.getId());
            ps.setInt(8, clientId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DELETE
    public boolean deleteReservation(int id) {
        String sql = "DELETE FROM reservation WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DELETE (OWNERSHIP-SAFE)
    public boolean deleteReservationOwnedByClient(int id, int clientId) {
        String sql = "DELETE FROM reservation WHERE id=? AND client_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setInt(2, clientId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

