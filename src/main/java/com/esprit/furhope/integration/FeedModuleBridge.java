package com.esprit.furhope.integration;

import com.esprit.furhope.utils.AppSession;

public final class FeedModuleBridge {

    private FeedModuleBridge() {
    }

    public static void applyAuthenticatedUser(int userId, String displayName) {
        AppSession.setCurrentUser(userId, displayName);
    }

    public static void applyDatabaseConfig(String url, String user, String password) {
        if (url != null && !url.isBlank()) {
            System.setProperty("furhope.db.url", url.trim());
        }
        if (user != null && !user.isBlank()) {
            System.setProperty("furhope.db.user", user.trim());
        }
        if (password != null) {
            System.setProperty("furhope.db.password", password);
        }
    }
}
