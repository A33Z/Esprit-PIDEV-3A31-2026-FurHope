package com.esprit.utils;

import com.esprit.config.DatabaseConfig;
import services.DatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        try {
            Connection connection = DriverManager.getConnection(
                    DatabaseConfig.jdbcUrl(),
                    DatabaseConfig.user(),
                    DatabaseConfig.password()
            );
            DatabaseSchemaService.ensureSecuritySchema(connection);
            return connection;
        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to connect to MariaDB at " + DatabaseConfig.jdbcUrl()
                            + " with user '" + DatabaseConfig.user() + "'. Cause: " + e.getMessage(),
                    e
            );
        }
    }

    public static void runStartupHealthCheck() {
        try {
            ensureDriverLoaded();
            try (Connection connection = DriverManager.getConnection(
                    DatabaseConfig.jdbcUrl(),
                    DatabaseConfig.user(),
                    DatabaseConfig.password());
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                System.out.println("DB OK");
            }
        } catch (SQLException e) {
            Throwable root = e.getCause() != null ? e.getCause() : e;
            System.err.println("DB ERROR: " + root.getMessage());
        }
    }

    private static void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MariaDB JDBC driver is not available on the classpath.", e);
        }
    }
}
