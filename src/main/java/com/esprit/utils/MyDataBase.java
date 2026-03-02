package com.esprit.utils;

import java.sql.Connection;
import java.sql.SQLException;

public class MyDataBase {

    private final Connection connection;
    private static volatile MyDataBase instance;

    private MyDataBase() {
        Connection tempConnection = null;
        try {
            tempConnection = DBConnection.getConnection();
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
        connection = tempConnection;
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            synchronized (MyDataBase.class) {
                if (instance == null) {
                    instance = new MyDataBase();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
