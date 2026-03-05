package integrations.auth;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class TwilioSmsSender {
    private static final Properties APP_PROPS = loadAppProperties();

    private TwilioSmsSender() {}

    public static boolean isConfigured() {
        return !isBlank(resolve("TWILIO_ACCOUNT_SID", "twilio.account_sid"))
                && !isBlank(resolve("TWILIO_AUTH_TOKEN", "twilio.auth_token"))
                && !isBlank(resolve("TWILIO_FROM_NUMBER", "twilio.from_number"));
    }

    public static String sendSms(String toPhone, String message) {
        String accountSid = resolve("TWILIO_ACCOUNT_SID", "twilio.account_sid");
        String authToken = resolve("TWILIO_AUTH_TOKEN", "twilio.auth_token");
        String fromNumber = resolve("TWILIO_FROM_NUMBER", "twilio.from_number");

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
            throw new IllegalStateException(
                    "Twilio is not configured. Set env vars TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER, " +
                            "or create infrastructure/secrets/twilio.secrets.properties (legacy root file still supported) in: "
                            + System.getProperty("user.dir")
            );
        }

        Twilio.init(accountSid, authToken);
        Message twilioMessage = Message.creator(new PhoneNumber(toPhone), new PhoneNumber(fromNumber), message).create();
        System.out.println("[TWILIO SMS] Message sent! SID=" + twilioMessage.getSid() + " status=" + twilioMessage.getStatus());
        return twilioMessage.getSid();
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

        // Load classpath defaults first (src/main/resources/application.properties)
        try (InputStream input = TwilioSmsSender.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
        }

        // Load local secrets if present; environment variables still override.
        loadOptionalFile(properties, "infrastructure/secrets/twilio.secrets.properties");
        loadOptionalFile(properties, "twilio.secrets.properties"); // legacy fallback

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
