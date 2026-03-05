package security;

import entities.Role;
import entities.User;

public final class SessionContext {

    private static User currentUser;

    private SessionContext() {
    }

    public static void loginAsManager() {
        currentUser = new User(0, "Hotel Manager", Role.HOTEL_MANAGER);
    }

    public static void loginAsUser(int userId) {
        currentUser = new User(userId, "User #" + userId, Role.USER);
    }

    public static User requireUser() {
        if (currentUser == null) {
            throw new AuthorizationException("No active session.");
        }
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }
}
