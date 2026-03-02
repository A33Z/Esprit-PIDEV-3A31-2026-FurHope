package com.esprit.furhope.integration.api;

public final class ApiConfig {

    public static final String BASE_URL = resolveBaseUrl();

    private ApiConfig() {
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

        return "http://127.0.0.1:8081";
    }
}
