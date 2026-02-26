package com.esprit.furhope.utils;

public final class AppSession {

    private static volatile int currentUserId = 1;
    private static volatile String currentDisplayName = "User 1";

    private AppSession() {
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentDisplayName() {
        return currentDisplayName;
    }

    public static void setCurrentUser(int userId, String displayName) {
        currentUserId = userId;
        currentDisplayName = (displayName == null || displayName.isBlank())
                ? ("User " + userId)
                : displayName;
    }

    public static void initializeFromSystemProperties() {
        String userIdProp = System.getProperty("furhope.user.id");
        String userNameProp = System.getProperty("furhope.user.name");

        if (userIdProp != null && !userIdProp.isBlank()) {
            try {
                int parsed = Integer.parseInt(userIdProp.trim());
                setCurrentUser(parsed, userNameProp);
                return;
            } catch (NumberFormatException ignored) {
                // keep default session
            }
        }

        if (userNameProp != null && !userNameProp.isBlank()) {
            currentDisplayName = userNameProp.trim();
        }
    }
}
