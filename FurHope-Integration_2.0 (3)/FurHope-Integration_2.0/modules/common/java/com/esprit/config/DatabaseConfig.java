package com.esprit.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static String host() {
        return read("db.host", "DB_HOST", "127.0.0.1");
    }

    public static int port() {
        return readInt("db.port", "DB_PORT", 3306);
    }

    public static String name() {
        return read("db.name", "DB_NAME", "hamza");
    }

    public static String user() {
        return read("db.user", "DB_USER", "safwen");
    }

    public static String password() {
        String propertyValue = PROPERTIES.getProperty("db.password");
        if (propertyValue != null) {
            String trimmed = propertyValue.trim();
            if (!"CHANGE_ME".equals(trimmed)) {
                return trimmed;
            }
        }
        String envValue = System.getenv("DB_PASSWORD");
        if (envValue != null && !"CHANGE_ME".equals(envValue.trim())) {
            return envValue.trim();
        }
        return "";
    }

    public static String params() {
        return read("db.params", null, "useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");
    }

    public static String jdbcUrl() {
        String base = "jdbc:mariadb://" + host() + ":" + port() + "/" + name();
        String query = params();
        if (query == null || query.isBlank()) {
            return base;
        }
        return base + "?" + query;
    }

    private static String read(String propertyKey, String envKey, String fallback) {
        String value = PROPERTIES.getProperty(propertyKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        if (envKey != null) {
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.trim().isEmpty()) {
                return envValue.trim();
            }
        }
        return fallback;
    }

    private static int readInt(String propertyKey, String envKey, int fallback) {
        String value = read(propertyKey, envKey, Integer.toString(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.err.println("DB config warning: unable to load " + CONFIG_FILE + " (" + e.getMessage() + ")");
        }
        return properties;
    }
}
