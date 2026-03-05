package integrations.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordResetService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(45);

    private static final Map<String, OtpEntry> OTP_BY_EMAIL = new ConcurrentHashMap<>();

    private final OtpService otpService = new OtpService();

    public void sendOtp(String email, String normalizedPhone) {
        sendOtpBySmsAndEmail(email, normalizedPhone);
    }

    public void sendOtpBySms(String email, String normalizedPhone) {
        String key = normalizeEmail(email);
        ensureCooldown(key);
        String code = otpService.generateOtpCode();
        String message = "FurHope reset code: " + code + ". It expires in 10 minutes.";
        TwilioSmsSender.sendSms(normalizedPhone, message);
        storeEntry(key, code);
    }

    public void sendOtpByEmail(String email) {
        String key = normalizeEmail(email);
        ensureCooldown(key);
        String code = otpService.generateOtpCode();
        String subject = "Your FurHope reset code";
        String body = "FurHope reset code: " + code + ". It expires in 10 minutes.";
        SendGridEmailSender.sendEmail(email, subject, body);
        storeEntry(key, code);
    }

    public DeliveryResult sendOtpBySmsAndEmail(String email, String normalizedPhone) {
        String key = normalizeEmail(email);
        ensureCooldown(key);

        boolean smsSent = false;
        boolean emailSent = false;
        Exception smsError = null;
        Exception emailError = null;

        String code = otpService.generateOtpCode();
        String smsMessage = "FurHope reset code: " + code + ". It expires in 10 minutes.";
        String subject = "Your FurHope reset code";
        String body = "FurHope reset code: " + code + ". It expires in 10 minutes.";

        if (normalizedPhone != null && !normalizedPhone.isBlank() && TwilioSmsSender.isConfigured()) {
            try {
                TwilioSmsSender.sendSms(normalizedPhone, smsMessage);
                smsSent = true;
            } catch (Exception e) {
                smsError = e;
            }
        }

        if (email != null && !email.isBlank() && SendGridEmailSender.isConfigured()) {
            try {
                SendGridEmailSender.sendEmail(email, subject, body);
                emailSent = true;
            } catch (Exception e) {
                emailError = e;
            }
        }

        if (!smsSent && !emailSent) {
            String smsReason = smsError == null ? "SMS channel unavailable." : smsError.getMessage();
            String emailReason = emailError == null ? "Email channel unavailable." : emailError.getMessage();
            throw new RuntimeException("Unable to send reset code. " + smsReason + " " + emailReason);
        }

        storeEntry(key, code);
        return new DeliveryResult(smsSent, emailSent, smsError, emailError);
    }

    public boolean verifyOtp(String email, String code) {
        String key = normalizeEmail(email);
        OtpEntry entry = OTP_BY_EMAIL.get(key);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            OTP_BY_EMAIL.remove(key);
            return false;
        }
        if (!entry.code.equals(code == null ? "" : code.trim())) {
            entry.failedAttempts++;
            if (entry.failedAttempts >= MAX_ATTEMPTS) {
                OTP_BY_EMAIL.remove(key);
            }
            return false;
        }
        return true;
    }

    public void clear(String email) {
        OTP_BY_EMAIL.remove(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void storeEntry(String key, String code) {
        OtpEntry entry = new OtpEntry();
        entry.code = code;
        entry.expiresAt = Instant.now().plus(OTP_VALIDITY);
        entry.sentAt = Instant.now();
        entry.failedAttempts = 0;
        OTP_BY_EMAIL.put(key, entry);
    }

    private void ensureCooldown(String key) {
        OtpEntry existing = OTP_BY_EMAIL.get(key);
        if (existing != null && Duration.between(existing.sentAt, Instant.now()).compareTo(RESEND_COOLDOWN) < 0) {
            throw new IllegalStateException("Please wait before requesting a new code.");
        }
    }

    public static final class DeliveryResult {
        private final boolean smsSent;
        private final boolean emailSent;
        private final Exception smsError;
        private final Exception emailError;

        public DeliveryResult(boolean smsSent, boolean emailSent, Exception smsError, Exception emailError) {
            this.smsSent = smsSent;
            this.emailSent = emailSent;
            this.smsError = smsError;
            this.emailError = emailError;
        }

        public boolean isSmsSent() {
            return smsSent;
        }

        public boolean isEmailSent() {
            return emailSent;
        }

        public Exception getSmsError() {
            return smsError;
        }

        public Exception getEmailError() {
            return emailError;
        }
    }

    private static final class OtpEntry {
        private String code;
        private Instant expiresAt;
        private Instant sentAt;
        private int failedAttempts;
    }
}
