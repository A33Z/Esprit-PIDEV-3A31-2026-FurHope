package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.services.auth.JwtService;
import io.jsonwebtoken.Claims;

public final class SessionContext {

    private static User currentUser;
    private static Integer selectedReclamationId;
    private static String authToken;
    private static final JwtService JWT_SERVICE = new JwtService();

    private SessionContext() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user == null) {
            authToken = null;
        } else {
            authToken = JWT_SERVICE.generateToken(user);
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
        selectedReclamationId = null;
        authToken = null;
    }

    public static boolean isLoggedIn() {
        if (currentUser == null || authToken == null || authToken.isBlank()) {
            return false;
        }
        boolean valid = JWT_SERVICE.isTokenValid(authToken);
        if (!valid) {
            clear();
        }
        return valid;
    }

    public static boolean isAdmin() {
        if (!isLoggedIn()) {
            return false;
        }
        try {
            Claims claims = JWT_SERVICE.parseClaims(authToken);
            String role = claims.get("role", String.class);
            return "ADMIN".equalsIgnoreCase(role);
        } catch (Exception e) {
            return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        }
    }

    public static void setSelectedReclamationId(Integer reclamationId) {
        selectedReclamationId = reclamationId;
    }

    public static Integer getSelectedReclamationId() {
        return selectedReclamationId;
    }

    public static String getAuthToken() {
        return authToken;
    }
}
