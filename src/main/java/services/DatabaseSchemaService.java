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
        ensureReservationTable(connection);
        ensureManagerAccountTable(connection);
        ensureReservationStatusIntegrity(connection);
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
                        WHEN UPPER(status) = 'CANCELLED' THEN 'DECLINED'
                        WHEN UPPER(status) IN ('PENDING', 'APPROVED', 'DECLINED') THEN UPPER(status)
                        ELSE 'PENDING'
                    END
                    """
            );

            statement.executeUpdate(
                    "ALTER TABLE reservation MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'"
            );
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
}
