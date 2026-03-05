package services;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseSchemaService {

    private static boolean initialized;

    private DatabaseSchemaService() {
    }

    public static synchronized void ensureSecuritySchema(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }
        ensureHotelTable(connection);
        ensureVetReviewTable(connection);
        ensureReservationTable(connection);
        ensureReservationMetadata(connection);
        ensureManagerAccountTable(connection);
        ensureReservationStatusIntegrity(connection);
        ensureUserProfileImageColumn(connection);
        initialized = true;
    }

    private static void ensureHotelTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS hotel (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(180) NOT NULL,
                    address VARCHAR(255) NOT NULL,
                    manager_id INT NOT NULL DEFAULT 0,
                    capacity INT NOT NULL DEFAULT 0
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureReservationTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS reservation (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    client_id INT NOT NULL,
                    animal_id INT NOT NULL,
                    hotel_id BIGINT NOT NULL,
                    reservation_date DATE NOT NULL,
                    guest_count INT NOT NULL DEFAULT 1,
                    nightly_rate DECIMAL(10,2) NOT NULL DEFAULT 85.00,
                    total_price DECIMAL(10,2) NOT NULL DEFAULT 85.00,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                    CONSTRAINT fk_reservation_hotel
                        FOREIGN KEY (hotel_id) REFERENCES hotel(id)
                        ON DELETE CASCADE
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureReservationMetadata(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "reservation", "reservation_date")) {
                statement.executeUpdate("ALTER TABLE reservation ADD COLUMN reservation_date DATE NULL");
            }
            statement.executeUpdate(
                    """
                    UPDATE reservation
                    SET reservation_date = COALESCE(reservation_date, start_date, CURRENT_DATE)
                    WHERE reservation_date IS NULL
                    """
            );
            statement.executeUpdate("ALTER TABLE reservation MODIFY COLUMN reservation_date DATE NOT NULL");

            if (!columnExists(connection, "reservation", "guest_count")) {
                statement.executeUpdate("ALTER TABLE reservation ADD COLUMN guest_count INT NULL DEFAULT 1");
            }
            statement.executeUpdate(
                    """
                    UPDATE reservation
                    SET guest_count = 1
                    WHERE guest_count IS NULL OR guest_count <= 0
                    """
            );
            statement.executeUpdate("ALTER TABLE reservation MODIFY COLUMN guest_count INT NOT NULL DEFAULT 1");

            if (!columnExists(connection, "reservation", "nightly_rate")) {
                statement.executeUpdate("ALTER TABLE reservation ADD COLUMN nightly_rate DECIMAL(10,2) NULL DEFAULT 85.00");
            }
            statement.executeUpdate(
                    """
                    UPDATE reservation
                    SET nightly_rate = 85.00
                    WHERE nightly_rate IS NULL OR nightly_rate <= 0
                    """
            );
            statement.executeUpdate("ALTER TABLE reservation MODIFY COLUMN nightly_rate DECIMAL(10,2) NOT NULL DEFAULT 85.00");

            if (!columnExists(connection, "reservation", "total_price")) {
                statement.executeUpdate("ALTER TABLE reservation ADD COLUMN total_price DECIMAL(10,2) NULL DEFAULT 85.00");
            }
            statement.executeUpdate(
                    """
                    UPDATE reservation
                    SET total_price = ROUND(GREATEST(DATEDIFF(end_date, start_date), 1) * nightly_rate, 2)
                    WHERE total_price IS NULL OR total_price < 0
                    """
            );
            statement.executeUpdate("ALTER TABLE reservation MODIFY COLUMN total_price DECIMAL(10,2) NOT NULL DEFAULT 85.00");
        }
    }

    private static void ensureManagerAccountTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS manager_account (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    manager_id VARCHAR(64) NOT NULL UNIQUE,
                    display_name VARCHAR(128) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    password_salt VARCHAR(255) NOT NULL,
                    password_iterations INT NOT NULL,
                    is_active TINYINT(1) NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void ensureReservationStatusIntegrity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "reservation", "status")) {
                statement.executeUpdate(
                        "ALTER TABLE reservation ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'"
                );
            } else {
                // Legacy schemas often define status as ENUM(PENDING,CONFIRMED,CANCELLED).
                // Convert to VARCHAR first so approved/declined normalization cannot be rejected.
                statement.executeUpdate("ALTER TABLE reservation MODIFY COLUMN status VARCHAR(16) NULL");
            }

            statement.executeUpdate(
                    """
                    UPDATE reservation
                    SET status = CASE
                        WHEN status IS NULL OR TRIM(status) = '' THEN 'PENDING'
                        WHEN UPPER(status) = 'CONFIRMED' THEN 'APPROVED'
                        WHEN UPPER(status) IN ('PENDING', 'APPROVED', 'DECLINED', 'CANCELLED') THEN UPPER(status)
                        ELSE 'PENDING'
                    END
                    """
            );

            statement.executeUpdate(
                    "ALTER TABLE reservation MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'"
            );
        }
    }

    private static void ensureUserProfileImageColumn(Connection connection) throws SQLException {
        if (columnExists(connection, "user", "profile_image_path")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE user ADD COLUMN profile_image_path VARCHAR(1024) NULL"
            );
        }
    }

    private static void ensureVetReviewTable(Connection connection) throws SQLException {
        String createSql = """
                CREATE TABLE IF NOT EXISTS review (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    client_id INT NOT NULL,
                    vet_id INT NOT NULL,
                    rdv_id INT NOT NULL,
                    rating INT NOT NULL,
                    commentaire TEXT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql);

            if (!columnExists(connection, "review", "commentaire")) {
                statement.executeUpdate("ALTER TABLE review ADD COLUMN commentaire TEXT NULL");
            }
            if (!columnExists(connection, "review", "created_at")) {
                statement.executeUpdate(
                        "ALTER TABLE review ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                );
            }
            if (!indexExists(connection, "review", "idx_review_vet")) {
                statement.executeUpdate("CREATE INDEX idx_review_vet ON review(vet_id)");
            }
            if (!indexExists(connection, "review", "idx_review_rdv")) {
                statement.executeUpdate("CREATE INDEX idx_review_rdv ON review(rdv_id)");
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, table.toUpperCase(), column.toUpperCase())) {
            return resultSet.next();
        }
    }

    private static boolean indexExists(Connection connection, String table, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (resultSet.next()) {
                String existingName = resultSet.getString("INDEX_NAME");
                if (existingName != null && existingName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, table.toUpperCase(), false, false)) {
            while (resultSet.next()) {
                String existingName = resultSet.getString("INDEX_NAME");
                if (existingName != null && existingName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
