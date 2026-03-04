package com.esprit.utils;

import com.esprit.config.DatabaseConfig;
import services.DatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MariaDB JDBC driver is not available on the classpath.", e);
        }

        String password = DatabaseConfig.password();
        if ("CHANGE_ME".equals(password)) {
            throw new SQLException("Database password is not configured. Set db.password or DB_PASSWORD.");
        }

        Connection connection = DriverManager.getConnection(
                DatabaseConfig.jdbcUrl(),
                DatabaseConfig.user(),
                password
        );
        DatabaseSchemaService.ensureSecuritySchema(connection);
        return connection;
    }

    public static void runStartupHealthCheck() {
        try (Connection ignored = getConnection()) {
            System.out.println("Database startup health check passed.");
        } catch (SQLException e) {
            throw new IllegalStateException("Database startup health check failed.", e);
        }
    }
}
