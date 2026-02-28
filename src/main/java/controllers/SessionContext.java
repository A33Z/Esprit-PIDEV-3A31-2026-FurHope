package controllers;

import entities.User;

public final class SessionContext {

    private static User currentUser;
    private static Integer selectedReclamationId;

    private SessionContext() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
        selectedReclamationId = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    public static void setSelectedReclamationId(Integer reclamationId) {
        selectedReclamationId = reclamationId;
    }

    public static Integer getSelectedReclamationId() {
        return selectedReclamationId;
    }
}

