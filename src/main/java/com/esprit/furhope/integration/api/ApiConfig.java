package com.esprit.furhope.integration.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApiConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = loadProperties();

    private ApiConfig() {
    }

    public static String baseUrl() {
        return resolveBaseUrl();
    }

    private static String resolveBaseUrl() {
        String fromProperty = System.getProperty("furhope.api.base-url");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }

        String fromEnv = System.getenv("FURHOPE_API_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromConfig = PROPERTIES.getProperty("api.base-url");
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig.trim();
        }

        return "http://127.0.0.1:8081";
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = ApiConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
            // Keep defaults when config file is unavailable.
        }
        return properties;
    }
}
