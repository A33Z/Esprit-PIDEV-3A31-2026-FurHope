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

    public static String getDisplayName() {
        return currentDisplayName;
    }

    public static void setCurrentUserId(long userId) {
        currentUserId = (int) userId;
    }

    public static void setDisplayName(String displayName) {
        currentDisplayName = (displayName == null || displayName.isBlank())
                ? ("User " + currentUserId)
                : displayName;
    }

    public static void setCurrentUser(int userId, String displayName) {
        setCurrentUserId(userId);
        setDisplayName(displayName);
    }

    public static void clear() {
        currentUserId = 0;
        currentDisplayName = "Guest";
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
