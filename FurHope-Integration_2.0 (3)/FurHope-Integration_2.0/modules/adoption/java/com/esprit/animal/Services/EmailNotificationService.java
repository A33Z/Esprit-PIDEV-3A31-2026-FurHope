package com.esprit.animal.Services;

import com.esprit.animal.config.ConfigManager;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.utils.MyDataBase;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class EmailNotificationService {

    private final Connection con;

    private final String smtpEnabled;
    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String smtpFrom;
    private final String smtpAuth;
    private final String smtpStartTls;

    public EmailNotificationService() {
        this.con = MyDataBase.getInstance().getConnection();
        this.smtpEnabled = readConfig("smtp.enabled");
        this.smtpHost = readConfig("smtp.host");
        this.smtpPort = readConfig("smtp.port");
        this.smtpUsername = readConfig("smtp.username");
        this.smtpPassword = readConfig("smtp.password");
        this.smtpFrom = readConfig("smtp.from");
        this.smtpAuth = readConfig("smtp.auth");
        this.smtpStartTls = readConfig("smtp.starttls");
    }

    public NotificationResult sendDecisionNotification(adoptionRequest request, adoptionRequest.status decision) {
        if (request == null) {
            return NotificationResult.failed("Missing adoption request.");
        }
        if (decision != adoptionRequest.status.APPROVED && decision != adoptionRequest.status.REJECTED) {
            return NotificationResult.failed("Unsupported request status for email notification.");
        }

        RequestEmailContext context = resolveContext(request);
        if (isBlank(context.recipientEmail)) {
            return NotificationResult.failed("No recipient email found for this request.");
        }
        if (!isSmtpConfigured()) {
            return NotificationResult.failed("SMTP is disabled or not fully configured.");
        }

        String animalName = isBlank(context.animalName) ? "animal" : context.animalName;
        String subject = decision == adoptionRequest.status.APPROVED
                ? "Adoption Request Approved"
                : "Adoption Request Declined";
        String body = buildBody(decision, animalName);

        try {
            sendEmail(context.recipientEmail, subject, body);
            return NotificationResult.success("Notification email sent to " + context.recipientEmail + ".");
        } catch (Exception e) {
            return NotificationResult.failed("Failed to send email: " + e.getMessage());
        }
    }

    private RequestEmailContext resolveContext(adoptionRequest request) {
        String email = null;
        String animalName = null;

        if (request.getClientCompte() != null && request.getClientCompte().getUser() != null) {
            email = request.getClientCompte().getUser().getEmail();
        }
        if (request.getAnimal() != null) {
            animalName = request.getAnimal().getName();
        }

        if (!isBlank(email) && !isBlank(animalName)) {
            return new RequestEmailContext(email, animalName);
        }

        RequestEmailContext dbContext = loadContextFromDatabase(request);
        if (!isBlank(email) && !isBlank(dbContext.animalName)) {
            return new RequestEmailContext(email, dbContext.animalName);
        }
        if (!isBlank(dbContext.recipientEmail) && !isBlank(animalName)) {
            return new RequestEmailContext(dbContext.recipientEmail, animalName);
        }
        return new RequestEmailContext(
                !isBlank(email) ? email : dbContext.recipientEmail,
                !isBlank(animalName) ? animalName : dbContext.animalName
        );
    }

    private RequestEmailContext loadContextFromDatabase(adoptionRequest request) {
        if (con == null) {
            return RequestEmailContext.empty();
        }

        String sqlById = "SELECT u.email AS client_email, a.name AS animal_name " +
                "FROM adoptionrequest r " +
                "LEFT JOIN compte c ON r.client_compte_id = c.id_compte " +
                "LEFT JOIN user u ON c.user_id = u.id_user " +
                "LEFT JOIN animal a ON r.animal_id = a.idAnimal " +
                "WHERE r.id = ?";

        String sqlByPair = "SELECT u.email AS client_email, a.name AS animal_name " +
                "FROM adoptionrequest r " +
                "LEFT JOIN compte c ON r.client_compte_id = c.id_compte " +
                "LEFT JOIN user u ON c.user_id = u.id_user " +
                "LEFT JOIN animal a ON r.animal_id = a.idAnimal " +
                "WHERE r.client_compte_id = ? AND r.animal_id = ? " +
                "ORDER BY r.id DESC LIMIT 1";

        try {
            if (request.getId() > 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlById)) {
                    ps.setInt(1, request.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new RequestEmailContext(
                                    clean(rs.getString("client_email")),
                                    clean(rs.getString("animal_name"))
                            );
                        }
                    }
                }
            }

            if (request.getClientCompteId() > 0 && request.getAnimal_id() > 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlByPair)) {
                    ps.setInt(1, request.getClientCompteId());
                    ps.setInt(2, request.getAnimal_id());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new RequestEmailContext(
                                    clean(rs.getString("client_email")),
                                    clean(rs.getString("animal_name"))
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Email context DB lookup failed: " + e.getMessage());
        }

        return RequestEmailContext.empty();
    }

    private void sendEmail(String recipient, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", smtpStartTls);

        boolean authEnabled = Boolean.parseBoolean(smtpAuth);
        jakarta.mail.Session mailSession;

        if (authEnabled) {
            mailSession = jakarta.mail.Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            mailSession = jakarta.mail.Session.getInstance(props);
        }

        Message message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(isBlank(smtpFrom) ? smtpUsername : smtpFrom));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }

    private String buildBody(adoptionRequest.status decision, String animalName) {
        if (decision == adoptionRequest.status.APPROVED) {
            return "Hello,\n\n" +
                    "Your adoption request for the animal \"" + animalName + "\" has been approved.\n\n" +
                    "You can now contact the owner to continue the adoption process.\n\n" +
                    "Best regards,\n" +
                    "Animal Adoption Platform";
        }

        return "Hello,\n\n" +
                "Unfortunately your adoption request for the animal \"" + animalName + "\" has been declined.\n\n" +
                "Thank you for using the platform.\n\n" +
                "Best regards,\n" +
                "Animal Adoption Platform";
    }

    private boolean isSmtpConfigured() {
        return Boolean.parseBoolean(smtpEnabled)
                && !isBlank(smtpHost)
                && !isBlank(smtpPort)
                && !isBlank(smtpUsername)
                && !isBlank(smtpPassword)
                && !isBlank(isBlank(smtpFrom) ? smtpUsername : smtpFrom);
    }

    private String readConfig(String key) {
        return clean(ConfigManager.get(key));
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class RequestEmailContext {
        private final String recipientEmail;
        private final String animalName;

        private RequestEmailContext(String recipientEmail, String animalName) {
            this.recipientEmail = recipientEmail;
            this.animalName = animalName;
        }

        private static RequestEmailContext empty() {
            return new RequestEmailContext(null, null);
        }
    }

    public static final class NotificationResult {
        private final boolean sent;
        private final String message;

        private NotificationResult(boolean sent, String message) {
            this.sent = sent;
            this.message = message;
        }

        public boolean isSent() {
            return sent;
        }

        public String getMessage() {
            return message;
        }

        public static NotificationResult success(String message) {
            return new NotificationResult(true, message);
        }

        public static NotificationResult failed(String message) {
            return new NotificationResult(false, message);
        }
    }
}

