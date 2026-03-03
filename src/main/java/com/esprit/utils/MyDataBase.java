package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private final String URL = "jdbc:mariadb://localhost:3306/FurHope3";
    private final String USER = "root";
    private final String PASSWORD = "";

    private final Connection connection;
    private static volatile MyDataBase instance;

    private MyDataBase() {
        Connection tempConnection = null;
        try {
            tempConnection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection failed:");
            e.printStackTrace();
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
