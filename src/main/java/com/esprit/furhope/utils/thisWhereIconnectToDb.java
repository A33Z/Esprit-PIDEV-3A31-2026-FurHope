package com.esprit.furhope.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class thisWhereIconnectToDb {

    private final String USERNAME = "safwen";
    private final String PASSWORD = "0473821736s"; //
    private final String URL = "jdbc:mysql://127.0.0.1:3306/furhope_db?useSSL=false&serverTimezone=UTC";

    private Connection connection;
    private static thisWhereIconnectToDb instance;

    private thisWhereIconnectToDb() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println(" tunnel DB Connection established ");
        } catch (SQLException e) {
            System.out.println(" DB Error: " + e.getMessage());
        }
    }

    public static thisWhereIconnectToDb getInstance() {
        if (instance == null) {
            instance = new thisWhereIconnectToDb();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
