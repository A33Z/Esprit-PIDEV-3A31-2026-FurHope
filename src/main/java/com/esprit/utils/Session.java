package com.esprit.utils;

public class Session {

    private static int userId;
    private static String userName;
    private static int userPhone;
    private static String userEmail;
    private static String userRole;

    // ✅ Setters and getters
    public static void setUserId(int id) { userId = id; }
    public static int getUserId() { return userId; }

    public static void setUserName(String name) { userName = name; }
    public static String getUserName() { return userName; }

    public static void setUserPhone(int phone) { userPhone = phone; }
    public static int getUserPhone() { return userPhone; }

    public static void setUserEmail(String email) { userEmail = email; }
    public static String getUserEmail() { return userEmail; }

    public static void setUserRole(String role) { userRole = role; }
    public static String getUserRole() { return userRole; }

    public static boolean isAdmin() {
        return "Client1".equals(userRole);
    }

    public static boolean isClient() {
        return "Client2".equals(userRole);
    }

    // ✅ Logout clears all session info
    public static void logout() {
        userId = 0;
        userName = null;
        userPhone = 0;
        userEmail = null;
        userRole = null;
    }
}