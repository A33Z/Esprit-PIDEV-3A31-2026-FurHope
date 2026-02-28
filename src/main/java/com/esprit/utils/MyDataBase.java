package com.esprit.utils;

import services.DatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MyDataBase {

    private static final String URL = "jdbc:mysql://localhost:3306/FurHope2";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDataBase instance;
    private Connection connection;

    private MyDataBase() {
        this.connection = openConnection();
    }

    public static synchronized MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = openConnection();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to verify database connection state.", e);
        }
        return connection;
    }

    private Connection openConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection opened = DriverManager.getConnection(URL, USER, PASSWORD);
            DatabaseSchemaService.ensureSecuritySchema(opened);
            return opened;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is not available on the classpath.", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to database.", e);
        }
    }
}
