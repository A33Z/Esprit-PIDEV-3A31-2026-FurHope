package services;

import entities.Hotel;
import com.esprit.utils.DBConnection;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HotelService {

    private static final int MAX_HOTEL_NAME_LENGTH = 180;
    private static final int MAX_HOTEL_ADDRESS_LENGTH = 255;

    private final Connection connection;

    public HotelService() {
        try {
            connection = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // CREATE
    public boolean addHotel(Hotel hotel) {
        validateHotelInput(hotel, false);
        String sql = "INSERT INTO hotel (name, address, manager_id, capacity) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, hotel.getName());
            ps.setString(2, hotel.getAddress());
            ps.setInt(3, hotel.getManagerId());
            ps.setInt(4, hotel.getCapacity());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Hotel> getAllHotels() {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Hotel hotel = new Hotel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getInt("manager_id"),
                        rs.getInt("capacity")
                );
                hotels.add(hotel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hotels;
    }

    // READ BY ID
    public Hotel getHotelById(int id) {
        String sql = "SELECT * FROM hotel WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Hotel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getInt("manager_id"),
                        rs.getInt("capacity")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // UPDATE
    public boolean updateHotel(Hotel hotel) {
        validateHotelInput(hotel, true);
        String sql = "UPDATE hotel SET name=?, address=?, manager_id=?, capacity=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, hotel.getName());
            ps.setString(2, hotel.getAddress());
            ps.setInt(3, hotel.getManagerId());
            ps.setInt(4, hotel.getCapacity());
            ps.setInt(5, hotel.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DELETE
    public boolean deleteHotel(int id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM hotel WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int ensureHotelRecord(String name, String address) {
        String normalizedName = sanitizeForDatabase(name, "Hotel", MAX_HOTEL_NAME_LENGTH);
        String normalizedAddress = sanitizeForDatabase(address, "Unknown location", MAX_HOTEL_ADDRESS_LENGTH);

        Integer existingId = findHotelIdByNameAndAddress(normalizedName, normalizedAddress);
        if (existingId != null) {
            return existingId;
        }

        Integer preferredManagerId = resolveDefaultManagerId();
        Integer insertedId = insertHotelRecord(normalizedName, normalizedAddress, preferredManagerId);
        if (insertedId != null) {
            return insertedId;
        }

        if (preferredManagerId != null && preferredManagerId != 0) {
            insertedId = insertHotelRecord(normalizedName, normalizedAddress, 0);
            if (insertedId != null) {
                return insertedId;
            }
        }

        // Fallback for legacy DB encodings that cannot store full Unicode payloads.
        String portableName = sanitizeForDatabase(toPortableText(normalizedName, "Hotel"), "Hotel", MAX_HOTEL_NAME_LENGTH);
        String portableAddress = sanitizeForDatabase(
                toPortableText(normalizedAddress, "Unknown location"),
                "Unknown location",
                MAX_HOTEL_ADDRESS_LENGTH
        );
        if (!portableName.equals(normalizedName) || !portableAddress.equals(normalizedAddress)) {
            Integer portableExisting = findHotelIdByNameAndAddress(portableName, portableAddress);
            if (portableExisting != null) {
                return portableExisting;
            }

            Integer portableInserted = insertHotelRecord(portableName, portableAddress, preferredManagerId);
            if (portableInserted != null) {
                return portableInserted;
            }
            if (preferredManagerId != null && preferredManagerId != 0) {
                portableInserted = insertHotelRecord(portableName, portableAddress, 0);
                if (portableInserted != null) {
                    return portableInserted;
                }
            }
        }

        Integer retryId = findHotelIdByNameAndAddress(normalizedName, normalizedAddress);
        if (retryId != null) {
            return retryId;
        }
        Integer retryPortableId = findHotelIdByNameAndAddress(portableName, portableAddress);
        if (retryPortableId != null) {
            return retryPortableId;
        }
        throw new IllegalStateException("Unable to ensure hotel record.");
    }

    private Integer findHotelIdByNameAndAddress(String name, String address) {
        String sql = """
                SELECT id
                FROM hotel
                WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))
                  AND LOWER(TRIM(address)) = LOWER(TRIM(?))
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<Integer, HotelGeoPoint> getHotelCoordinatesIfAvailable() {
        String latitudeColumn = resolveCoordinateColumn("latitude", "lat", "hotel_latitude");
        String longitudeColumn = resolveCoordinateColumn("longitude", "lng", "lon", "hotel_longitude");
        if (latitudeColumn == null || longitudeColumn == null) {
            return Map.of();
        }

        String sql = "SELECT id, " + latitudeColumn + ", " + longitudeColumn + " FROM hotel";
        Map<Integer, HotelGeoPoint> points = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                int hotelId = rs.getInt("id");
                double latitude = rs.getDouble(latitudeColumn);
                if (rs.wasNull()) {
                    continue;
                }
                double longitude = rs.getDouble(longitudeColumn);
                if (rs.wasNull()) {
                    continue;
                }
                if (!isValidCoordinate(latitude, longitude)) {
                    continue;
                }
                points.put(hotelId, new HotelGeoPoint(latitude, longitude));
            }
        } catch (SQLException ignored) {
            return Map.of();
        }
        return points;
    }

    private Integer insertHotelRecord(String name, String address, Integer managerId) {
        String sql = "INSERT INTO hotel (name, address, manager_id, capacity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setInt(3, managerId == null ? 0 : managerId);
            ps.setInt(4, 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    private Integer resolveDefaultManagerId() {
        Integer fromHotel = querySingleInt("SELECT manager_id FROM hotel WHERE manager_id > 0 ORDER BY manager_id LIMIT 1");
        if (fromHotel != null) {
            return fromHotel;
        }

        ForeignKeyTarget target = managerForeignKeyTarget();
        if (target == null) {
            return 0;
        }

        String table = safeIdentifier(target.tableName());
        String column = safeIdentifier(target.columnName());
        if (table == null || column == null) {
            return 0;
        }
        return querySingleInt("SELECT " + column + " FROM " + table + " ORDER BY " + column + " LIMIT 1");
    }

    private ForeignKeyTarget managerForeignKeyTarget() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet keys = metaData.getImportedKeys(connection.getCatalog(), null, "hotel")) {
                while (keys.next()) {
                    String fkColumn = keys.getString("FKCOLUMN_NAME");
                    if (fkColumn != null && fkColumn.equalsIgnoreCase("manager_id")) {
                        return new ForeignKeyTarget(
                                keys.getString("PKTABLE_NAME"),
                                keys.getString("PKCOLUMN_NAME")
                        );
                    }
                }
            }
            try (ResultSet keys = metaData.getImportedKeys(connection.getCatalog(), null, "HOTEL")) {
                while (keys.next()) {
                    String fkColumn = keys.getString("FKCOLUMN_NAME");
                    if (fkColumn != null && fkColumn.equalsIgnoreCase("manager_id")) {
                        return new ForeignKeyTarget(
                                keys.getString("PKTABLE_NAME"),
                                keys.getString("PKCOLUMN_NAME")
                        );
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private Integer querySingleInt(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                int value = rs.getInt(1);
                return rs.wasNull() ? null : value;
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private String safeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.matches("[A-Za-z0-9_]+") ? trimmed : null;
    }

    private String resolveCoordinateColumn(String... candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }

        Map<String, String> columnsByLower = loadHotelColumnsByLowerName();
        if (columnsByLower.isEmpty()) {
            return null;
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String actual = columnsByLower.get(candidate.toLowerCase(Locale.US));
            if (actual != null) {
                String safe = safeIdentifier(actual);
                if (safe != null) {
                    return safe;
                }
            }
        }
        return null;
    }

    private Map<String, String> loadHotelColumnsByLowerName() {
        Map<String, String> columns = new HashMap<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, "hotel", null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName == null || columnName.isBlank()) {
                        continue;
                    }
                    columns.putIfAbsent(columnName.toLowerCase(Locale.US), columnName);
                }
            }
            if (!columns.isEmpty()) {
                return columns;
            }
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, "HOTEL", null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName == null || columnName.isBlank()) {
                        continue;
                    }
                    columns.putIfAbsent(columnName.toLowerCase(Locale.US), columnName);
                }
            }
        } catch (SQLException ignored) {
            return Map.of();
        }
        return columns;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private void validateHotelInput(Hotel hotel, boolean idRequired) {
        if (hotel == null) {
            throw new IllegalArgumentException("Hotel is required.");
        }
        if (idRequired && hotel.getId() <= 0) {
            throw new IllegalArgumentException("Hotel ID is invalid.");
        }
        if (hotel.getName() == null || hotel.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Hotel name is required.");
        }
        if (hotel.getAddress() == null || hotel.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Hotel address is required.");
        }
        if (hotel.getManagerId() < 0) {
            throw new IllegalArgumentException("Manager ID is invalid.");
        }
        if (hotel.getCapacity() < 0) {
            throw new IllegalArgumentException("Capacity is invalid.");
        }
    }

    private String requireNormalized(String value, String error) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(error);
        }
        return value.trim();
    }

    private String sanitizeForDatabase(String value, String fallback, int maxLength) {
        String normalized = value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String toPortableText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum} .,'#&()/_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        return normalized;
    }

    private record ForeignKeyTarget(String tableName, String columnName) {
    }

    public record HotelGeoPoint(double latitude, double longitude) {
    }
}


