package com.esprit.utils;

import services.DatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/FurHope2";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver is not available on the classpath.", e);
        }
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        DatabaseSchemaService.ensureSecuritySchema(connection);
        return connection;
    }
}
