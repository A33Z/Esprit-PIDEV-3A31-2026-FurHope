package com.esprit.furhope.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class thisWhereIconnectToDb {

    private final String USERNAME = readSetting("furhope.db.user", "FURHOPE_DB_USER", "root");
    private final String PASSWORD = readSetting("furhope.db.password", "FURHOPE_DB_PASSWORD", "");
    private final String URL = readSetting(
            "furhope.db.url",
            "FURHOPE_DB_URL",
            "jdbc:mysql://127.0.0.1:3306/furhope_db?useSSL=false&serverTimezone=UTC"
    );

    private Connection connection;
    private static thisWhereIconnectToDb instance;

    private thisWhereIconnectToDb() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("DB connection established");
        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
            System.out.println("DB config used -> url: " + URL + ", user: " + USERNAME);
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

    private String readSetting(String propKey, String envKey, String fallback) {
        String propValue = System.getProperty(propKey);
        if (propValue != null && !propValue.isBlank()) return propValue.trim();

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) return envValue.trim();

        return fallback;
    }
}
