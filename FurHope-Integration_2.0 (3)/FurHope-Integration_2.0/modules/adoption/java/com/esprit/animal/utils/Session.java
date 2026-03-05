package com.esprit.animal.utils;

public class Session {

    private static int compteId;
    private static int userId;
    private static String userName;
    private static int userPhone;
    private static String userEmail;
    private static String userRole;

    public static void setCompteId(int id) {
        compteId = id;
    }

    public static int getCompteId() {
        return compteId > 0 ? compteId : userId;
    }

    public static void setUserId(int id) {
        userId = id;
        if (compteId <= 0) {
            compteId = id;
        }
    }

    public static int getUserId() {
        return userId > 0 ? userId : compteId;
    }

    public static void setUserName(String name) {
        userName = name;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserPhone(int phone) {
        userPhone = phone;
    }

    public static int getUserPhone() {
        return userPhone;
    }

    public static void setUserEmail(String email) {
        userEmail = email;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserRole(String role) {
        userRole = role;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static boolean isAdmin() {
        return "Client1".equals(userRole);
    }

    public static boolean isClient() {
        return "Client2".equals(userRole);
    }

    public static void logout() {
        compteId = 0;
        userId = 0;
        userName = null;
        userPhone = 0;
        userEmail = null;
        userRole = null;
    }
}

