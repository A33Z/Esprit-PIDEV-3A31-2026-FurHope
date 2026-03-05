package com.projet.payment.service;

import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class PaymentEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEmailService.class);
    private static final Properties LOCAL_PROPS = loadLocalProperties();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final boolean mockMode;
    private final String brevoApiKey;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;

    public PaymentEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${payment.mail.from:no-reply@petshop.local}") String fromAddress,
            @Value("${payment.mail.mock:true}") boolean mockMode,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = firstNonBlank(
                resolve("MAIL_FROM_ADDRESS", "mail.from_address"),
                fromAddress
        );
        this.fromName = firstNonBlank(
                resolve("MAIL_FROM_NAME", "mail.from_name"),
                "FurHope"
        );
        this.mockMode = mockMode;
        this.brevoApiKey = resolve("BREVO_API_KEY", "mail.brevo.api_key");
        this.smtpHost = firstNonBlank(
                resolve("PAYMENT_SMTP_HOST", "mail.smtp.host"),
                resolve("SMTP_HOST", "spring.mail.host")
        );
        this.smtpPort = parsePort(firstNonBlank(
                resolve("PAYMENT_SMTP_PORT", "mail.smtp.port"),
                resolve("SMTP_PORT", "spring.mail.port"),
                "587"
        ), 587);
        this.smtpUsername = firstNonBlank(
                resolve("PAYMENT_SMTP_USERNAME", "mail.smtp.username"),
                resolve("SMTP_USERNAME", "spring.mail.username"),
                smtpUsername
        );
        this.smtpPassword = firstNonBlank(
                resolve("PAYMENT_SMTP_PASSWORD", "mail.smtp.password"),
                resolve("SMTP_PASSWORD", "spring.mail.password"),
                smtpPassword
        );
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void sendConfirmationCode(String to, String transactionId, String code) {
        String subject = "Pet Shop Payment Confirmation";
        String body = "Your payment verification code is: " + code
                + "\nTransaction ID: " + transactionId
                + "\nThis code expires in 10 minutes.";
        sendEmail(to, subject, body);
    }

    public void sendPaymentSuccessConfirmation(String to, String transactionId, String code, BigDecimal amount) {
        String formattedAmount = amount == null ? "N/A" : amount.toPlainString();
        String subject = "FurHope Payment Successful";
        String body = "Your payment has been confirmed successfully."
                + "\nTransaction ID: " + transactionId
                + "\nAmount: " + formattedAmount
                + "\nConfirmation code: " + code
                + "\nEverything is set. Merci cher client, and come buy again at FurHope.";
        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (mockMode) {
            logger.info("MOCK MAIL to={} subject={}", to, subject);
            return;
        }

        List<String> failures = new ArrayList<>();

        if (isSmtpConfigured()) {
            try {
                sendWithSmtp(to, subject, body);
                return;
            } catch (Exception e) {
                String detail = buildErrorChain(e);
                failures.add("SMTP: " + detail);
                logger.warn("SMTP delivery failed, trying Brevo fallback: {}", detail);
            }
        }

        if (!isBlank(brevoApiKey)) {
            try {
                sendWithBrevo(to, subject, body);
                return;
            } catch (Exception e) {
                String detail = buildErrorChain(e);
                failures.add("Brevo: " + detail);
                logger.warn("Brevo delivery failed: {}", detail);
            }
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Email delivery failed. " + String.join(" | ", failures));
        }

        if (!isBlank(smtpHost)) {
            throw new IllegalStateException("SMTP host is set but username/password are missing.");
        }

        throw new IllegalStateException(
                "Email provider is not configured. Set a valid BREVO_API_KEY or SMTP settings (PAYMENT_SMTP_HOST/USERNAME/PASSWORD)."
        );
    }

    private void sendWithBrevo(String toEmail, String subject, String textBody) {
        String payload = "{"
                + "\"sender\":{\"email\":\"" + escapeJson(fromAddress) + "\",\"name\":\"" + escapeJson(fromName) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"textContent\":\"" + escapeJson(textBody) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", brevoApiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Brevo API error (" + code + "): " + safeValue(response.body()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Brevo request interrupted.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Brevo request failed: " + safeValue(e.getMessage()), e);
        }
    }

    private void sendWithSmtp(String toEmail, String subject, String textBody) {
        JavaMailSender sender = mailSender;
        if (sender == null) {
            JavaMailSenderImpl manualSender = new JavaMailSenderImpl();
            manualSender.setHost(smtpHost);
            manualSender.setPort(smtpPort);
            if (!isBlank(smtpUsername)) {
                manualSender.setUsername(smtpUsername);
            }
            if (!isBlank(smtpPassword)) {
                manualSender.setPassword(smtpPassword);
            }

            Properties javaMailProps = new Properties();
            javaMailProps.put("mail.smtp.auth", "true");
            javaMailProps.put("mail.smtp.starttls.enable", "true");
            javaMailProps.put("mail.smtp.connectiontimeout", "5000");
            javaMailProps.put("mail.smtp.timeout", "5000");
            javaMailProps.put("mail.smtp.writetimeout", "5000");
            manualSender.setJavaMailProperties(javaMailProps);
            sender = manualSender;
        }

        if (isBlank(smtpUsername) || isBlank(smtpPassword)) {
            throw new IllegalStateException("SMTP credentials are missing (username/password).");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        String effectiveFrom = fromAddress;
        if (isBlank(effectiveFrom) || effectiveFrom.endsWith(".local")) {
            effectiveFrom = smtpUsername;
        }
        message.setFrom(firstNonBlank(effectiveFrom, smtpUsername));
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(textBody);
        sender.send(message);
    }

    private static String resolve(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }
        String propValue = LOCAL_PROPS.getProperty(propertyKey);
        return propValue == null ? null : propValue.trim();
    }

    private static Properties loadLocalProperties() {
        Properties props = new Properties();

        try (InputStream in = PaymentEmailService.class.getClassLoader().getResourceAsStream("payment-api.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
        }

        try (InputStream in = PaymentEmailService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
        }

        loadOptionalFile(props, "infrastructure/secrets/mail.secrets.properties");
        loadOptionalFile(props, "mail.secrets.properties"); // legacy fallback

        return props;
    }

    private static void loadOptionalFile(Properties props, String path) {
        try {
            File file = new File(path);
            if (file.isFile()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean isSmtpConfigured() {
        return !isBlank(smtpHost) && !isBlank(smtpUsername) && !isBlank(smtpPassword);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private static String buildErrorChain(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (builder.length() > 0) {
                builder.append(" -> ");
            }
            builder.append(current.getClass().getSimpleName());
            if (!isBlank(current.getMessage())) {
                builder.append(": ").append(safeValue(current.getMessage()));
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    private static String safeValue(String value) {
        if (isBlank(value)) {
            return "";
        }
        String compact = value.replace('\r', ' ').replace('\n', ' ').trim();
        return compact.length() > 400 ? compact.substring(0, 400) + "..." : compact;
    }
}
