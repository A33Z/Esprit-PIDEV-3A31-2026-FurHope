package com.esprit.config;

public final class AppConfig {

    private static final String DEFAULT_USER_AGENT = "FurHope/1.0 (Desktop App)";
    private static final String DEFAULT_CITY = "New York";

    private AppConfig() {
    }

    public static String httpUserAgent() {
        return read("FURHOPE_HTTP_USER_AGENT", DEFAULT_USER_AGENT);
    }

    public static String defaultCity() {
        return read("FURHOPE_DEFAULT_CITY", DEFAULT_CITY);
    }

    public static int hotelSearchLimit() {
        return readInt("FURHOPE_HOTEL_LIMIT", 8, 3, 20);
    }

    public static int overpassRadiusMeters() {
        return readInt("FURHOPE_OVERPASS_RADIUS", 7000, 1000, 20000);
    }

    private static String read(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static int readInt(String key, int fallback, int min, int max) {
        String raw = System.getenv(key);
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
