package services;

import entities.ManagerAccount;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;

public class ManagerAuthService {

    private static final String STATIC_MANAGER_ID = readFromEnv("FURHOPE_MANAGER_ID", "admin");
    private static final String STATIC_MANAGER_PASSWORD = readFromEnv("FURHOPE_MANAGER_PASSWORD", "admin");
    private static final String STATIC_MANAGER_DISPLAY_NAME = readFromEnv("FURHOPE_MANAGER_DISPLAY_NAME", "System Admin");

    public Optional<ManagerAccount> authenticate(String rawManagerId, char[] password) {
        String managerId = normalize(rawManagerId);
        if (!ManagerIdPolicy.isValid(managerId) || password == null || password.length == 0) {
            return Optional.empty();
        }

        byte[] providedId = managerId.getBytes(StandardCharsets.UTF_8);
        byte[] expectedId = STATIC_MANAGER_ID.getBytes(StandardCharsets.UTF_8);
        byte[] providedPassword = new String(password).getBytes(StandardCharsets.UTF_8);
        byte[] expectedPassword = STATIC_MANAGER_PASSWORD.getBytes(StandardCharsets.UTF_8);

        try {
            boolean idMatch = MessageDigest.isEqual(providedId, expectedId);
            boolean passwordMatch = MessageDigest.isEqual(providedPassword, expectedPassword);
            if (!idMatch || !passwordMatch) {
                return Optional.empty();
            }
            return Optional.of(new ManagerAccount(STATIC_MANAGER_ID, STATIC_MANAGER_DISPLAY_NAME));
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(providedPassword, (byte) 0);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String readFromEnv(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
