package services;

import entities.ManagerAccount;
import entities.Role;
import entities.User;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public final class SessionContext {

    private static final Duration MANAGER_SESSION_TIMEOUT = Duration.ofMinutes(20);
    private static final Duration USER_SESSION_TIMEOUT = Duration.ofHours(12);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static SessionState currentSession;

    private SessionContext() {
    }

    public static synchronized void startManagerSession(ManagerAccount account) {
        if (account == null) {
            throw new AuthorizationException("Authentication failed.");
        }
        User manager = new User(0, account.getDisplayName(), Role.HOTEL_MANAGER, account.getManagerId());
        currentSession = createSession(manager, MANAGER_SESSION_TIMEOUT);
    }

    public static synchronized void startUserSession(int userId) {
        if (userId <= 0) {
            throw new AuthorizationException("Invalid user identifier.");
        }
        User user = new User(userId, "User #" + userId, Role.USER, "USER-" + userId);
        currentSession = createSession(user, USER_SESSION_TIMEOUT);
    }

    public static synchronized User requireUser() {
        if (currentSession == null) {
            throw new AuthorizationException("No active session.");
        }
        if (Instant.now().isAfter(currentSession.expiresAt())) {
            logout();
            throw new AuthorizationException("Session expired. Please sign in again.");
        }

        currentSession = currentSession.touch();
        return currentSession.user();
    }

    public static User requireManager() {
        User user = requireUser();
        if (!user.hasRole(Role.HOTEL_MANAGER)) {
            throw new AuthorizationException("Manager privileges are required.");
        }
        return user;
    }

    public static User requireNormalUser() {
        User user = requireUser();
        if (!user.hasRole(Role.USER)) {
            throw new AuthorizationException("User privileges are required.");
        }
        return user;
    }

    public static synchronized void logout() {
        currentSession = null;
    }

    public static synchronized boolean hasActiveSession() {
        try {
            requireUser();
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
    }

    private static SessionState createSession(User user, Duration timeToLive) {
        Instant now = Instant.now();
        return new SessionState(
                user,
                generateSessionId(),
                now,
                now.plus(timeToLive),
                timeToLive
        );
    }

    private static String generateSessionId() {
        byte[] raw = new byte[24];
        SECURE_RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private record SessionState(User user, String sessionId, Instant issuedAt, Instant expiresAt, Duration ttl) {
        private SessionState touch() {
            Instant now = Instant.now();
            return new SessionState(user, sessionId, issuedAt, now.plus(ttl), ttl);
        }
    }
}
