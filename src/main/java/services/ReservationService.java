package services;

import entities.Reservation;
import entities.ReservationStatus;
import com.esprit.utils.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReservationService {

    private static final BigDecimal DEFAULT_NIGHTLY_RATE = new BigDecimal("85.00");
    private static final String CONFLICT_MESSAGE = "Reservation dates conflict with an existing booking.";
    private static final BigDecimal OCCUPANCY_THRESHOLD_PERCENT = new BigDecimal("70.00");
    private static final int ANALYTICS_TREND_MONTHS = 6;
    private static final String[] GUEST_ADJECTIVES = {"Calm", "Bright", "Kind", "Swift", "Gentle", "Bold", "Sunny", "Noble"};
    private static final String[] GUEST_NOUNS = {"Traveler", "Explorer", "Companion", "Visitor", "Guest", "Nomad", "Adventurer", "Voyager"};

    private final Connection connection;
    private final DynamicPricingEngine dynamicPricingEngine = new DynamicPricingEngine();

    public ReservationService() {
        try {
            connection = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addReservation(Reservation reservation) {
        validateReservationInput(reservation);
        Date reservationDate = reservation.getReservationDate() == null
                ? Date.valueOf(LocalDate.now())
                : reservation.getReservationDate();
        reservation.setReservationDate(reservationDate);
        BigDecimal baseNightlyRate = sanitizeNightlyRate(reservation.getNightlyRate());
        reservation.setNightlyRate(baseNightlyRate);

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }

        try {
            connection.setAutoCommit(false);

            assertNoReservationConflict(
                    reservation.getHotelId(),
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    null
            );

            boolean occupancyAboveThreshold = isOccupancyAboveThreshold(
                    reservation.getHotelId(),
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    reservation.getGuestCount(),
                    null
            );
            reservation.setTotalPrice(dynamicPricingEngine.calculateFinalTotal(
                    baseNightlyRate,
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    occupancyAboveThreshold
            ));

            String sql = """
                    INSERT INTO reservation (client_id, animal_id, hotel_id, reservation_date, guest_count, nightly_rate, total_price, start_date, end_date, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, reservation.getClientId());
                ps.setInt(2, reservation.getAnimalId());
                ps.setInt(3, reservation.getHotelId());
                ps.setDate(4, reservationDate);
                ps.setInt(5, reservation.getGuestCount());
                ps.setBigDecimal(6, reservation.getNightlyRate());
                ps.setBigDecimal(7, reservation.getTotalPrice());
                ps.setDate(8, reservation.getStartDate());
                ps.setDate(9, reservation.getEndDate());
                ps.setString(10, ReservationStatus.PENDING.name());
                boolean inserted = ps.executeUpdate() > 0;
                connection.commit();
                return inserted;
            }
        } catch (ReservationConflictException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation ORDER BY reservation_date DESC, id DESC";

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
        String sql = "SELECT * FROM reservation WHERE client_id = ? ORDER BY reservation_date DESC, id DESC";

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

        return modifyReservationDatesOwnedByClient(
                reservation.getId(),
                clientId,
                reservation.getStartDate(),
                reservation.getEndDate()
        ).code() == UserReservationActionCode.UPDATED;
    }

    public boolean deleteReservationOwnedByClient(int reservationId, int clientId) {
        return cancelReservationOwnedByClient(reservationId, clientId).code() == UserReservationActionCode.CANCELLED;
    }

    public UserReservationActionResult modifyReservationDatesOwnedByClient(
            int reservationId,
            int clientId,
            Date checkInDate,
            Date checkOutDate
    ) {
        if (reservationId <= 0 || clientId <= 0) {
            return UserReservationActionResult.notFound();
        }
        if (checkInDate == null || checkOutDate == null || !checkOutDate.after(checkInDate)) {
            return UserReservationActionResult.invalidDates();
        }

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return UserReservationActionResult.failed();
        }

        try {
            connection.setAutoCommit(false);

            Reservation existing = lockReservationById(reservationId);
            if (existing == null) {
                connection.rollback();
                return UserReservationActionResult.notFound();
            }
            if (existing.getClientId() != clientId) {
                connection.rollback();
                return UserReservationActionResult.forbidden();
            }
            if (existing.getStatus() != ReservationStatus.PENDING) {
                connection.rollback();
                return UserReservationActionResult.invalidStatus(existing);
            }

            assertNoReservationConflict(
                    existing.getHotelId(),
                    checkInDate,
                    checkOutDate,
                    reservationId
            );

            BigDecimal nightlyRate = sanitizeNightlyRate(existing.getNightlyRate());
            boolean occupancyAboveThreshold = isOccupancyAboveThreshold(
                    existing.getHotelId(),
                    checkInDate,
                    checkOutDate,
                    existing.getGuestCount(),
                    reservationId
            );
            BigDecimal totalPrice = dynamicPricingEngine.calculateFinalTotal(
                    nightlyRate,
                    checkInDate,
                    checkOutDate,
                    occupancyAboveThreshold
            );

            String updateSql = """
                    UPDATE reservation
                    SET start_date = ?, end_date = ?, status = ?, total_price = ?, nightly_rate = ?, reservation_date = CURRENT_DATE
                    WHERE id = ? AND client_id = ?
                    """;
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                updateStatement.setDate(1, checkInDate);
                updateStatement.setDate(2, checkOutDate);
                updateStatement.setString(3, ReservationStatus.PENDING.name());
                updateStatement.setBigDecimal(4, totalPrice);
                updateStatement.setBigDecimal(5, nightlyRate);
                updateStatement.setInt(6, reservationId);
                updateStatement.setInt(7, clientId);
                int updatedRows = updateStatement.executeUpdate();
                if (updatedRows <= 0) {
                    connection.rollback();
                    return UserReservationActionResult.notFound();
                }
            }

            Reservation updated = lockReservationById(reservationId);
            connection.commit();
            return UserReservationActionResult.updated(updated);
        } catch (ReservationConflictException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return UserReservationActionResult.failed();
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public UserReservationActionResult cancelReservationOwnedByClient(int reservationId, int clientId) {
        if (reservationId <= 0 || clientId <= 0) {
            return UserReservationActionResult.notFound();
        }

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return UserReservationActionResult.failed();
        }

        try {
            connection.setAutoCommit(false);

            Reservation existing = lockReservationById(reservationId);
            if (existing == null) {
                connection.rollback();
                return UserReservationActionResult.notFound();
            }
            if (existing.getClientId() != clientId) {
                connection.rollback();
                return UserReservationActionResult.forbidden();
            }
            if (existing.getStatus() == ReservationStatus.DECLINED || existing.getStatus() == ReservationStatus.CANCELLED) {
                connection.rollback();
                return UserReservationActionResult.invalidStatus(existing);
            }

            String updateSql = "UPDATE reservation SET status = ? WHERE id = ? AND client_id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                updateStatement.setString(1, ReservationStatus.CANCELLED.name());
                updateStatement.setInt(2, reservationId);
                updateStatement.setInt(3, clientId);
                int updatedRows = updateStatement.executeUpdate();
                if (updatedRows <= 0) {
                    connection.rollback();
                    return UserReservationActionResult.notFound();
                }
            }

            Reservation cancelled = lockReservationById(reservationId);
            connection.commit();
            return UserReservationActionResult.cancelled(cancelled);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return UserReservationActionResult.failed();
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public UserReservationActionResult modifyReservationDatesByManager(
            int reservationId,
            Date checkInDate,
            Date checkOutDate
    ) {
        if (reservationId <= 0) {
            return UserReservationActionResult.notFound();
        }
        if (checkInDate == null || checkOutDate == null || !checkOutDate.after(checkInDate)) {
            return UserReservationActionResult.invalidDates();
        }

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return UserReservationActionResult.failed();
        }

        try {
            connection.setAutoCommit(false);

            Reservation existing = lockReservationById(reservationId);
            if (existing == null) {
                connection.rollback();
                return UserReservationActionResult.notFound();
            }
            if (existing.getStatus() == ReservationStatus.DECLINED || existing.getStatus() == ReservationStatus.CANCELLED) {
                connection.rollback();
                return UserReservationActionResult.invalidStatus(existing);
            }

            assertNoReservationConflict(
                    existing.getHotelId(),
                    checkInDate,
                    checkOutDate,
                    reservationId
            );

            BigDecimal nightlyRate = sanitizeNightlyRate(existing.getNightlyRate());
            boolean occupancyAboveThreshold = isOccupancyAboveThreshold(
                    existing.getHotelId(),
                    checkInDate,
                    checkOutDate,
                    existing.getGuestCount(),
                    reservationId
            );
            BigDecimal totalPrice = dynamicPricingEngine.calculateFinalTotal(
                    nightlyRate,
                    checkInDate,
                    checkOutDate,
                    occupancyAboveThreshold
            );

            String updateSql = """
                    UPDATE reservation
                    SET start_date = ?, end_date = ?, status = ?, total_price = ?, nightly_rate = ?, reservation_date = CURRENT_DATE
                    WHERE id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setDate(1, checkInDate);
                statement.setDate(2, checkOutDate);
                statement.setString(3, ReservationStatus.PENDING.name());
                statement.setBigDecimal(4, totalPrice);
                statement.setBigDecimal(5, nightlyRate);
                statement.setInt(6, reservationId);
                int updatedRows = statement.executeUpdate();
                if (updatedRows <= 0) {
                    connection.rollback();
                    return UserReservationActionResult.notFound();
                }
            }

            Reservation updated = lockReservationById(reservationId);
            connection.commit();
            return UserReservationActionResult.updated(updated);
        } catch (ReservationConflictException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return UserReservationActionResult.failed();
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public UserReservationActionResult cancelReservationByManager(int reservationId) {
        if (reservationId <= 0) {
            return UserReservationActionResult.notFound();
        }

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return UserReservationActionResult.failed();
        }

        try {
            connection.setAutoCommit(false);

            Reservation existing = lockReservationById(reservationId);
            if (existing == null) {
                connection.rollback();
                return UserReservationActionResult.notFound();
            }
            if (existing.getStatus() == ReservationStatus.DECLINED || existing.getStatus() == ReservationStatus.CANCELLED) {
                connection.rollback();
                return UserReservationActionResult.invalidStatus(existing);
            }

            String updateSql = "UPDATE reservation SET status = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, ReservationStatus.CANCELLED.name());
                statement.setInt(2, reservationId);
                int updatedRows = statement.executeUpdate();
                if (updatedRows <= 0) {
                    connection.rollback();
                    return UserReservationActionResult.notFound();
                }
            }

            Reservation updated = lockReservationById(reservationId);
            connection.commit();
            return UserReservationActionResult.cancelled(updated);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return UserReservationActionResult.failed();
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public ReservationDecisionResult decideReservationStatusByManager(int reservationId, ReservationStatus targetStatus) {
        if (reservationId <= 0 || targetStatus == null) {
            return ReservationDecisionResult.invalidTarget();
        }
        if (targetStatus != ReservationStatus.APPROVED && targetStatus != ReservationStatus.DECLINED) {
            return ReservationDecisionResult.invalidTarget();
        }

        boolean initialAutoCommit;
        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            return ReservationDecisionResult.failed();
        }

        try {
            connection.setAutoCommit(false);

            ReservationStatus currentStatus;
            String lockSql = "SELECT status FROM reservation WHERE id = ? FOR UPDATE";
            try (PreparedStatement lockStatement = connection.prepareStatement(lockSql)) {
                lockStatement.setInt(1, reservationId);
                try (ResultSet rs = lockStatement.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return ReservationDecisionResult.notFound();
                    }
                    currentStatus = ReservationStatus.fromDatabase(rs.getString("status"));
                }
            }

            if (currentStatus != ReservationStatus.PENDING) {
                connection.rollback();
                return ReservationDecisionResult.alreadyProcessed(currentStatus);
            }

            String updateSql = "UPDATE reservation SET status = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                updateStatement.setString(1, targetStatus.name());
                updateStatement.setInt(2, reservationId);
                int updatedRows = updateStatement.executeUpdate();
                if (updatedRows <= 0) {
                    connection.rollback();
                    return ReservationDecisionResult.notFound();
                }
            }

            connection.commit();
            return ReservationDecisionResult.updated(targetStatus);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return ReservationDecisionResult.failed();
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean updateReservationStatusByManager(int reservationId, ReservationStatus targetStatus) {
        return decideReservationStatusByManager(reservationId, targetStatus).isUpdated();
    }

    public List<ManagerReservationSnapshot> getManagerReservationPage(int page, int pageSize) {
        if (page < 0 || pageSize <= 0) {
            return List.of();
        }

        int offset = page * pageSize;
        List<ManagerReservationSnapshot> tickets = new ArrayList<>();
        String sql = """
                SELECT
                    r.id,
                    r.client_id,
                    r.reservation_date,
                    r.start_date,
                    r.end_date,
                    r.total_price,
                    r.guest_count,
                    r.status,
                    COALESCE(NULLIF(TRIM(h.name), ''), CONCAT('Hotel #', r.hotel_id)) AS hotel_name
                FROM reservation r
                LEFT JOIN hotel h ON h.id = r.hotel_id
                ORDER BY r.reservation_date DESC, r.id DESC
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date checkInDate = rs.getDate("start_date");
                    Date checkOutDate = rs.getDate("end_date");
                    tickets.add(new ManagerReservationSnapshot(
                            rs.getInt("id"),
                            rs.getString("hotel_name"),
                            maskedGuestName(rs.getInt("client_id")),
                            rs.getDate("reservation_date"),
                            checkInDate,
                            checkOutDate,
                            calculateNightsSafe(checkInDate, checkOutDate),
                            normalizeMoney(rs.getBigDecimal("total_price")),
                            rs.getInt("guest_count"),
                            ReservationStatus.fromDatabase(rs.getString("status"))
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return tickets;
    }

    public int countAllReservations() {
        String sql = "SELECT COUNT(*) AS total FROM reservation";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    public ManagerAnalyticsSnapshot getManagerAnalyticsSnapshot() {
        BigDecimal totalRevenue = calculateTotalRevenue();
        int totalReservations = countAllReservations();
        String mostBookedHotel = resolveMostBookedHotel();
        double averageOccupancyRate = calculateAverageOccupancyRate();
        List<MonthlyReservationTrendPoint> monthlyTrend = loadMonthlyReservationTrend(ANALYTICS_TREND_MONTHS);
        List<MonthlyRevenueTrendPoint> revenueTrend = loadMonthlyRevenueTrend(ANALYTICS_TREND_MONTHS);
        return new ManagerAnalyticsSnapshot(
                totalRevenue,
                totalReservations,
                mostBookedHotel,
                averageOccupancyRate,
                monthlyTrend,
                revenueTrend
        );
    }

    private void assertNoReservationConflict(
            int hotelId,
            Date newCheckIn,
            Date newCheckOut,
            Integer excludeReservationId
    ) throws SQLException {
        String exclusionClause = excludeReservationId == null ? "" : "AND id <> ? ";
        String sql = """
                SELECT id
                FROM reservation
                WHERE hotel_id = ?
                  AND status IN (?, ?)
                  AND start_date <= ?
                  AND end_date >= ?
                """
                + exclusionClause
                + "LIMIT 1 FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            statement.setInt(idx++, hotelId);
            statement.setString(idx++, ReservationStatus.PENDING.name());
            statement.setString(idx++, ReservationStatus.APPROVED.name());
            statement.setDate(idx++, newCheckOut);
            statement.setDate(idx++, newCheckIn);
            if (excludeReservationId != null) {
                statement.setInt(idx, excludeReservationId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    throw new ReservationConflictException(CONFLICT_MESSAGE);
                }
            }
        }
    }

    private boolean isOccupancyAboveThreshold(
            int hotelId,
            Date checkInDate,
            Date checkOutDate,
            int incomingGuestCount,
            Integer excludeReservationId
    ) throws SQLException {
        int capacity = fetchHotelCapacity(hotelId);
        if (capacity <= 0) {
            return false;
        }

        int overlappingGuests = countOverlappingGuests(hotelId, checkInDate, checkOutDate, excludeReservationId);
        int projectedGuests = overlappingGuests + Math.max(incomingGuestCount, 0);
        BigDecimal projectedRate = BigDecimal.valueOf(projectedGuests)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
        return projectedRate.compareTo(OCCUPANCY_THRESHOLD_PERCENT) > 0;
    }

    private int fetchHotelCapacity(int hotelId) throws SQLException {
        String sql = "SELECT capacity FROM hotel WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("capacity"));
                }
            }
        }
        return 0;
    }

    private int countOverlappingGuests(
            int hotelId,
            Date checkInDate,
            Date checkOutDate,
            Integer excludeReservationId
    ) throws SQLException {
        String exclusionClause = excludeReservationId == null ? "" : "AND id <> ? ";
        String sql = """
                SELECT COALESCE(SUM(guest_count), 0) AS occupied_guests
                FROM reservation
                WHERE hotel_id = ?
                  AND status IN (?, ?)
                  AND start_date <= ?
                  AND end_date >= ?
                """
                + exclusionClause
                + "FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            statement.setInt(idx++, hotelId);
            statement.setString(idx++, ReservationStatus.PENDING.name());
            statement.setString(idx++, ReservationStatus.APPROVED.name());
            statement.setDate(idx++, checkOutDate);
            statement.setDate(idx++, checkInDate);
            if (excludeReservationId != null) {
                statement.setInt(idx, excludeReservationId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("occupied_guests"));
                }
            }
        }
        return 0;
    }

    private BigDecimal calculateTotalRevenue() {
        String sql = """
                SELECT COALESCE(SUM(total_price), 0) AS revenue
                FROM reservation
                WHERE status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ReservationStatus.APPROVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal revenue = rs.getBigDecimal("revenue");
                    if (revenue != null) {
                        return revenue.setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveMostBookedHotel() {
        String sql = """
                SELECT
                    COALESCE(NULLIF(TRIM(h.name), ''), CONCAT('Hotel #', r.hotel_id)) AS hotel_name,
                    COUNT(*) AS total_bookings
                FROM reservation r
                LEFT JOIN hotel h ON h.id = r.hotel_id
                GROUP BY r.hotel_id, hotel_name
                ORDER BY total_bookings DESC, hotel_name ASC
                LIMIT 1
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                String hotelName = rs.getString("hotel_name");
                return hotelName == null || hotelName.isBlank() ? "N/A" : hotelName.trim();
            }
        } catch (SQLException ignored) {
        }
        return "N/A";
    }

    private double calculateAverageOccupancyRate() {
        int totalCapacity = querySingleInt("SELECT COALESCE(SUM(capacity), 0) AS total_capacity FROM hotel");
        if (totalCapacity <= 0) {
            return 0.0;
        }

        String occupiedSql = """
                SELECT COALESCE(SUM(guest_count), 0) AS occupied_guests
                FROM reservation
                WHERE status IN (?, ?)
                  AND CURRENT_DATE BETWEEN start_date AND end_date
                """;
        int occupiedGuests = 0;
        try (PreparedStatement ps = connection.prepareStatement(occupiedSql)) {
            ps.setString(1, ReservationStatus.PENDING.name());
            ps.setString(2, ReservationStatus.APPROVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    occupiedGuests = Math.max(0, rs.getInt("occupied_guests"));
                }
            }
        } catch (SQLException ignored) {
        }

        double ratio = (occupiedGuests * 100.0) / totalCapacity;
        return Math.max(0.0, Math.round(ratio * 10.0) / 10.0);
    }

    private List<MonthlyReservationTrendPoint> loadMonthlyReservationTrend(int months) {
        int normalizedMonths = Math.max(1, months);
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(normalizedMonths - 1L);
        LocalDate startDate = startMonth.atDay(1);

        Map<YearMonth, Integer> totalsByMonth = new LinkedHashMap<>();
        for (int i = 0; i < normalizedMonths; i++) {
            YearMonth month = startMonth.plusMonths(i);
            totalsByMonth.put(month, 0);
        }

        String sql = """
                SELECT DATE_FORMAT(reservation_date, '%Y-%m') AS month_key, COUNT(*) AS total
                FROM reservation
                WHERE reservation_date >= ?
                GROUP BY DATE_FORMAT(reservation_date, '%Y-%m')
                ORDER BY month_key
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String monthKey = rs.getString("month_key");
                    if (monthKey == null || monthKey.isBlank()) {
                        continue;
                    }
                    YearMonth month = YearMonth.parse(monthKey);
                    if (totalsByMonth.containsKey(month)) {
                        totalsByMonth.put(month, Math.max(0, rs.getInt("total")));
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        List<MonthlyReservationTrendPoint> points = new ArrayList<>();
        for (Map.Entry<YearMonth, Integer> entry : totalsByMonth.entrySet()) {
            points.add(new MonthlyReservationTrendPoint(
                    entry.getKey().toString(),
                    entry.getValue()
            ));
        }
        return points;
    }

    private int querySingleInt(String sql) {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public Map<Integer, HotelAvailabilitySnapshot> getHotelAvailabilitySummary() {
        Map<Integer, HotelAvailabilitySnapshot> summary = new HashMap<>();
        String sql = """
                SELECT
                    h.id AS hotel_id,
                    h.capacity AS total_rooms,
                    COALESCE(SUM(
                        CASE
                            WHEN r.status IN (?, ?)
                             AND CURRENT_DATE BETWEEN r.start_date AND r.end_date
                            THEN r.guest_count
                            ELSE 0
                        END
                    ), 0) AS occupied_rooms
                FROM hotel h
                LEFT JOIN reservation r ON r.hotel_id = h.id
                GROUP BY h.id, h.capacity
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ReservationStatus.PENDING.name());
            ps.setString(2, ReservationStatus.APPROVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int hotelId = rs.getInt("hotel_id");
                    int totalRooms = Math.max(0, rs.getInt("total_rooms"));
                    int occupiedRooms = Math.max(0, rs.getInt("occupied_rooms"));
                    int availableRooms = Math.max(0, totalRooms - occupiedRooms);
                    double occupancyRate = totalRooms <= 0
                            ? 0.0
                            : Math.min(100.0, Math.round((occupiedRooms * 1000.0) / totalRooms) / 10.0);
                    summary.put(hotelId, new HotelAvailabilitySnapshot(
                            hotelId,
                            totalRooms,
                            occupiedRooms,
                            availableRooms,
                            occupancyRate,
                            availabilityStatusFromRate(occupancyRate, totalRooms)
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return summary;
    }

    private String availabilityStatusFromRate(double occupancyRate, int totalRooms) {
        if (totalRooms <= 0) {
            return "Unknown";
        }
        if (occupancyRate >= 90.0) {
            return "Nearly Full";
        }
        if (occupancyRate >= 70.0) {
            return "Limited";
        }
        if (occupancyRate > 0.0) {
            return "Available";
        }
        return "Open";
    }

    private List<MonthlyRevenueTrendPoint> loadMonthlyRevenueTrend(int months) {
        int normalizedMonths = Math.max(1, months);
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(normalizedMonths - 1L);
        LocalDate startDate = startMonth.atDay(1);

        Map<YearMonth, BigDecimal> totalsByMonth = new LinkedHashMap<>();
        for (int i = 0; i < normalizedMonths; i++) {
            totalsByMonth.put(startMonth.plusMonths(i), BigDecimal.ZERO);
        }

        String sql = """
                SELECT DATE_FORMAT(reservation_date, '%Y-%m') AS month_key, COALESCE(SUM(total_price), 0) AS total_revenue
                FROM reservation
                WHERE reservation_date >= ?
                  AND status = ?
                GROUP BY DATE_FORMAT(reservation_date, '%Y-%m')
                ORDER BY month_key
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setString(2, ReservationStatus.APPROVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String monthKey = rs.getString("month_key");
                    if (monthKey == null || monthKey.isBlank()) {
                        continue;
                    }
                    YearMonth month = YearMonth.parse(monthKey);
                    if (!totalsByMonth.containsKey(month)) {
                        continue;
                    }
                    totalsByMonth.put(month, normalizeMoney(rs.getBigDecimal("total_revenue")));
                }
            }
        } catch (SQLException ignored) {
        }

        List<MonthlyRevenueTrendPoint> points = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> entry : totalsByMonth.entrySet()) {
            points.add(new MonthlyRevenueTrendPoint(entry.getKey().toString(), normalizeMoney(entry.getValue())));
        }
        return points;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private long calculateNightsSafe(Date checkInDate, Date checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(checkInDate.toLocalDate(), checkOutDate.toLocalDate()));
    }

    private String maskedGuestName(int clientId) {
        int hash = Math.abs((clientId * 37) + 11);
        String adjective = GUEST_ADJECTIVES[hash % GUEST_ADJECTIVES.length];
        String noun = GUEST_NOUNS[(hash / GUEST_ADJECTIVES.length) % GUEST_NOUNS.length];
        return adjective + " " + noun;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getInt("animal_id"),
                rs.getInt("hotel_id"),
                rs.getDate("reservation_date"),
                rs.getDate("start_date"),
                rs.getDate("end_date"),
                rs.getInt("guest_count"),
                rs.getBigDecimal("nightly_rate"),
                rs.getBigDecimal("total_price"),
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
        Date reservationDate = reservation.getReservationDate();
        if (reservationDate == null) {
            reservation.setReservationDate(Date.valueOf(LocalDate.now()));
        }
        Date startDate = reservation.getStartDate();
        Date endDate = reservation.getEndDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Reservation dates are required.");
        }
        if (!endDate.after(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        if (reservation.getGuestCount() <= 0) {
            throw new IllegalArgumentException("Guest count must be greater than 0.");
        }
        reservation.setNightlyRate(sanitizeNightlyRate(reservation.getNightlyRate()));
    }

    private Reservation lockReservationById(int reservationId) throws SQLException {
        String sql = "SELECT * FROM reservation WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    private BigDecimal sanitizeNightlyRate(BigDecimal rawRate) {
        if (rawRate == null || rawRate.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_NIGHTLY_RATE;
        }
        return rawRate.setScale(2, RoundingMode.HALF_UP);
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

