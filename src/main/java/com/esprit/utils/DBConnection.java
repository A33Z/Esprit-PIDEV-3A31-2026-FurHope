package com.esprit.utils;

import services.DatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mariadb://localhost:3306/FurHope3";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MariaDB JDBC driver is not available on the classpath.", e);
        }
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
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
