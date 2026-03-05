package com.esprit.animal.utils;

import com.esprit.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private Connection connection;
    private static MyDataBase instance;


    // constructeur
    private MyDataBase() {
        connect();
    }

    public static MyDataBase getInstance() {
        if (instance == null){
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    private void connect() {
        try {
            String password = DatabaseConfig.password();
            if ("CHANGE_ME".equals(password)) {
                throw new SQLException("Database password is not configured.");
            }

            connection = DriverManager.getConnection(
                    DatabaseConfig.jdbcUrl(),
                    DatabaseConfig.user(),
                    password
            );
            System.out.println("Animal module DB connection established: " + DatabaseConfig.name());
        } catch (SQLException e) {
            System.out.println("Animal module DB connection error: " + e.getMessage());
        }
    }

}

