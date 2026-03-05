package integrations.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class  SendGridEmailSender {

    private static final Properties APP_PROPS = loadAppProperties();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private SendGridEmailSender() {}

    public static boolean isConfigured() {
        return !isBlank(resolve("BREVO_API_KEY", "mail.brevo.api_key"))
                && !isBlank(resolve("MAIL_FROM_ADDRESS", "mail.from_address"));
    }

    public static void sendEmail(String toEmail, String subject, String textBody) {
        String apiKey = resolve("BREVO_API_KEY", "mail.brevo.api_key");
        String fromAddress = resolve("MAIL_FROM_ADDRESS", "mail.from_address");
        String fromName = resolve("MAIL_FROM_NAME", "mail.from_name");

        if (isBlank(apiKey) || isBlank(fromAddress)) {
            throw new IllegalStateException(
                    "Email API is not configured. Set BREVO_API_KEY and MAIL_FROM_ADDRESS " +
                            "or create infrastructure/secrets/mail.secrets.properties (legacy root file still supported) in: "
                            + System.getProperty("user.dir")
            );
        }

        String payload = buildPayload(fromAddress, fromName, toEmail, subject, textBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Brevo API error (" + code + "): " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email reset code.", e);
        }
    }

    private static String buildPayload(String fromAddress, String fromName, String toEmail, String subject, String textBody) {
        String escapedFrom = escapeJson(fromAddress);
        String escapedName = escapeJson(defaultIfBlank(fromName, "FurHope"));
        String escapedTo = escapeJson(toEmail);
        String escapedSubject = escapeJson(subject);
        String escapedBody = escapeJson(textBody);

        return "{"
                + "\"sender\":{\"email\":\"" + escapedFrom + "\",\"name\":\"" + escapedName + "\"},"
                + "\"to\":[{\"email\":\"" + escapedTo + "\"}],"
                + "\"subject\":\"" + escapedSubject + "\","
                + "\"textContent\":\"" + escapedBody + "\""
                + "}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String resolve(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }
        String propValue = APP_PROPS.getProperty(propertyKey);
        return propValue == null ? null : propValue.trim();
    }

    private static Properties loadAppProperties() {
        Properties properties = new Properties();

        try (InputStream input = SendGridEmailSender.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
        }

        loadOptionalFile(properties, "infrastructure/secrets/mail.secrets.properties");
        loadOptionalFile(properties, "mail.secrets.properties"); // legacy fallback

        return properties;
    }

    private static void loadOptionalFile(Properties properties, String path) {
        try {
            File file = new File(path);
            if (file.isFile()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
