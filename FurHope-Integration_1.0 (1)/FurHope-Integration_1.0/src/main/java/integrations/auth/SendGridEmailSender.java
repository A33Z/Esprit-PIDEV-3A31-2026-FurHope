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
        String fromAddress = resolve("MAIL_FROM_ADDRESS", "mail.from_address");
        return !isBlank(fromAddress) && (!isBlank(resolveBrevoKey()) || !isBlank(resolveSendGridKey()));
    }

    public static void sendEmail(String toEmail, String subject, String textBody) {
        String fromAddress = resolve("MAIL_FROM_ADDRESS", "mail.from_address");
        String fromName = resolve("MAIL_FROM_NAME", "mail.from_name");

        if (isBlank(fromAddress)) {
            throw new IllegalStateException(
                    "Email API is not configured. Set MAIL_FROM_ADDRESS " +
                            "or create mail.secrets.properties in: " + System.getProperty("user.dir")
            );
        }

        String provider = resolve("MAIL_PROVIDER", "mail.provider");
        String brevoKey = resolveBrevoKey();
        String sendGridKey = resolveSendGridKey();

        boolean useSendGrid = "sendgrid".equalsIgnoreCase(provider)
                || (!isBlank(sendGridKey) && (isBlank(provider) || !"brevo".equalsIgnoreCase(provider)));

        if (useSendGrid) {
            if (isBlank(sendGridKey)) {
                throw new IllegalStateException("SendGrid is selected but SENDGRID_API_KEY is missing.");
            }
            sendViaSendGrid(sendGridKey, fromAddress, fromName, toEmail, subject, textBody);
            return;
        }

        if (isBlank(brevoKey)) {
            throw new IllegalStateException("Brevo is selected but BREVO_API_KEY is missing.");
        }
        sendViaBrevo(brevoKey, fromAddress, fromName, toEmail, subject, textBody);
    }

    private static void sendViaBrevo(String apiKey, String fromAddress, String fromName, String toEmail, String subject, String textBody) {
        String payload = buildBrevoPayload(fromAddress, fromName, toEmail, subject, textBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        sendRequest(request, "Brevo");
    }

    private static void sendViaSendGrid(String apiKey, String fromAddress, String fromName, String toEmail, String subject, String textBody) {
        String payload = buildSendGridPayload(fromAddress, fromName, toEmail, subject, textBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        sendRequest(request, "SendGrid");
    }

    private static void sendRequest(HttpRequest request, String providerLabel) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException(providerLabel + " API error (" + code + "): " + response.body());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email reset code via " + providerLabel + ": " + e.getMessage(), e);
        }
    }

    private static String buildBrevoPayload(String fromAddress, String fromName, String toEmail, String subject, String textBody) {
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

    private static String buildSendGridPayload(String fromAddress, String fromName, String toEmail, String subject, String textBody) {
        String escapedFrom = escapeJson(fromAddress);
        String escapedName = escapeJson(defaultIfBlank(fromName, "FurHope"));
        String escapedTo = escapeJson(toEmail);
        String escapedSubject = escapeJson(subject);
        String escapedBody = escapeJson(textBody);

        return "{"
                + "\"personalizations\":[{\"to\":[{\"email\":\"" + escapedTo + "\"}]}],"
                + "\"from\":{\"email\":\"" + escapedFrom + "\",\"name\":\"" + escapedName + "\"},"
                + "\"subject\":\"" + escapedSubject + "\","
                + "\"content\":[{\"type\":\"text/plain\",\"value\":\"" + escapedBody + "\"}]"
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

    private static String resolveBrevoKey() {
        return firstNonBlank(
                resolve("BREVO_API_KEY", "mail.brevo.api_key"),
                resolve("MAIL_BREVO_API_KEY", "mail.brevo.api_key")
        );
    }

    private static String resolveSendGridKey() {
        return firstNonBlank(
                resolve("SENDGRID_API_KEY", "mail.sendgrid.api_key"),
                resolve("MAIL_SENDGRID_API_KEY", "mail.sendgrid.api_key")
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static Properties loadAppProperties() {
        Properties properties = new Properties();

        try (InputStream input = SendGridEmailSender.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
        }

        try {
            File secrets = new File("mail.secrets.properties");
            if (secrets.isFile()) {
                try (FileInputStream in = new FileInputStream(secrets)) {
                    properties.load(in);
                }
            }
        } catch (Exception ignored) {
        }

        return properties;
    }
}
