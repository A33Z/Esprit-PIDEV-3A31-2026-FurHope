package com.esprit.services.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReclamationAutoReplyConfig {

    private final AutoReplyMode mode;
    private final int systemAdminId;
    private final String provider;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final double minimumConfidence;

    public ReclamationAutoReplyConfig(
            AutoReplyMode mode,
            int systemAdminId,
            String provider,
            String apiKey,
            String apiUrl,
            String model,
            double minimumConfidence
    ) {
        this.mode = mode;
        this.systemAdminId = systemAdminId;
        this.provider = provider;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.minimumConfidence = minimumConfidence;
    }

    public AutoReplyMode getMode() {
        return mode;
    }

    public int getSystemAdminId() {
        return systemAdminId;
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }

    public double getMinimumConfidence() {
        return minimumConfidence;
    }

    public boolean isOpenAiCompatibleEnabled() {
        return "OPENAI_COMPAT".equalsIgnoreCase(provider)
                && apiKey != null && !apiKey.isBlank()
                && apiUrl != null && !apiUrl.isBlank()
                && model != null && !model.isBlank();
    }

    public static ReclamationAutoReplyConfig load() {
        Properties properties = loadProperties();

        String modeValue = firstNonBlank(
                System.getenv("AI_REPLIES_MODE"),
                properties.getProperty("ai.replies.mode")
        );
        String adminIdValue = firstNonBlank(
                System.getenv("AI_REPLIES_SYSTEM_ADMIN_ID"),
                properties.getProperty("ai.replies.system_admin_id")
        );
        String provider = firstNonBlank(
                System.getenv("AI_REPLIES_PROVIDER"),
                properties.getProperty("ai.replies.provider"),
                "RULES"
        );
        String apiKey = firstNonBlank(
                System.getenv("OPENAI_API_KEY"),
                System.getenv("AI_REPLIES_API_KEY"),
                properties.getProperty("ai.replies.api_key")
        );
        String apiUrl = firstNonBlank(
                System.getenv("AI_REPLIES_API_URL"),
                properties.getProperty("ai.replies.api_url"),
                "https://api.openai.com/v1/chat/completions"
        );
        String model = firstNonBlank(
                System.getenv("AI_REPLIES_MODEL"),
                properties.getProperty("ai.replies.model"),
                "gpt-4o-mini"
        );
        double minConfidence = parseDouble(
                firstNonBlank(
                        System.getenv("AI_REPLIES_MIN_CONFIDENCE"),
                        properties.getProperty("ai.replies.min_confidence"),
                        "0.70"
                ),
                0.70
        );

        AutoReplyMode mode = AutoReplyMode.fromValue(modeValue);
        int adminId = parseInt(adminIdValue, -1);
        return new ReclamationAutoReplyConfig(
                mode,
                adminId,
                provider,
                apiKey,
                apiUrl,
                model,
                minConfidence
        );
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = ReclamationAutoReplyConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }

        loadOptionalFile(properties, "infrastructure/secrets/ai.secrets.properties");
        loadOptionalFile(properties, "ai.secrets.properties"); // legacy fallback
        return properties;
    }

    private static void loadOptionalFile(Properties properties, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
