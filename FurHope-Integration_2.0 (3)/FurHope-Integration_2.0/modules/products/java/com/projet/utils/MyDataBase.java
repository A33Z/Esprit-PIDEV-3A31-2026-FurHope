package com.projet.utils;

import com.esprit.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private final Connection connection;
    private static volatile MyDataBase instance;

    private MyDataBase() {
        Connection tempConnection = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            String password = DatabaseConfig.password();
            if ("CHANGE_ME".equals(password)) {
                throw new SQLException("Database password is not configured. Set db.password or DB_PASSWORD.");
            }
            tempConnection = DriverManager.getConnection(
                    DatabaseConfig.jdbcUrl(),
                    DatabaseConfig.user(),
                    password
            );
            System.out.println("Product module connected to database.");
        } catch (ClassNotFoundException e) {
            System.err.println("MariaDB JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Product module database connection failed.");
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
