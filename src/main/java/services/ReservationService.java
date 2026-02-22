package services;

import entities.Reservation;
import entities.ReservationStatus;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private final Connection connection;

    public ReservationService() {
        try {
            connection = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addReservation(Reservation reservation) {
        validateReservationInput(reservation);

        String sql = """
                INSERT INTO reservation (client_id, animal_id, hotel_id, start_date, end_date, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservation.getClientId());
            ps.setInt(2, reservation.getAnimalId());
            ps.setInt(3, reservation.getHotelId());
            ps.setDate(4, reservation.getStartDate());
            ps.setDate(5, reservation.getEndDate());
            ps.setString(6, ReservationStatus.PENDING.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Reservation> getReservationsByClientId(int clientId) {
        if (clientId <= 0) {
            return List.of();
        }

        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE client_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public Reservation getReservationById(int id) {
        if (id <= 0) {
            return null;
        }

        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateReservationOwnedByClient(Reservation reservation, int clientId) {
        validateReservationInput(reservation);
        if (reservation.getId() <= 0 || clientId <= 0) {
            return false;
        }

        String sql = """
                UPDATE reservation
                SET animal_id = ?, hotel_id = ?, start_date = ?, end_date = ?
                WHERE id = ? AND client_id = ? AND status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservation.getAnimalId());
            ps.setInt(2, reservation.getHotelId());
            ps.setDate(3, reservation.getStartDate());
            ps.setDate(4, reservation.getEndDate());
            ps.setInt(5, reservation.getId());
            ps.setInt(6, clientId);
            ps.setString(7, ReservationStatus.PENDING.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteReservationOwnedByClient(int reservationId, int clientId) {
        if (reservationId <= 0 || clientId <= 0) {
            return false;
        }

        String sql = "DELETE FROM reservation WHERE id = ? AND client_id = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, clientId);
            ps.setString(3, ReservationStatus.PENDING.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateReservationStatusByManager(int reservationId, ReservationStatus targetStatus) {
        if (reservationId <= 0 || targetStatus == null) {
            return false;
        }
        if (targetStatus == ReservationStatus.PENDING) {
            return false;
        }

        String sql = "UPDATE reservation SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetStatus.name());
            ps.setInt(2, reservationId);
            ps.setString(3, ReservationStatus.PENDING.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getInt("animal_id"),
                rs.getInt("hotel_id"),
                rs.getDate("start_date"),
                rs.getDate("end_date"),
                ReservationStatus.fromDatabase(rs.getString("status"))
        );
    }

    private void validateReservationInput(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required.");
        }
        if (reservation.getClientId() <= 0) {
            throw new IllegalArgumentException("Client ID is invalid.");
        }
        if (reservation.getAnimalId() <= 0) {
            throw new IllegalArgumentException("Animal ID is invalid.");
        }
        if (reservation.getHotelId() <= 0 || !hotelExists(reservation.getHotelId())) {
            throw new IllegalArgumentException("Hotel ID is invalid.");
        }
        Date startDate = reservation.getStartDate();
        Date endDate = reservation.getEndDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Reservation dates are required.");
        }
        if (endDate.before(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
    }

    private boolean hotelExists(int hotelId) {
        String sql = "SELECT 1 FROM hotel WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
