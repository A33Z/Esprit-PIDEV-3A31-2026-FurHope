package utils;

public final class SessionManager {

    private static int userId;
    private static String userNom;
    private static String userRole;
    private static int selectedVetId;
    private static String selectedVetNom;

    private SessionManager() {
    }

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int userId) {
        SessionManager.userId = userId;
    }

    public static String getUserNom() {
        return userNom == null ? "" : userNom;
    }

    public static void setUserNom(String userNom) {
        SessionManager.userNom = userNom;
    }

    public static String getUserRole() {
        return userRole == null ? "" : userRole;
    }

    public static void setUserRole(String userRole) {
        SessionManager.userRole = userRole;
    }

    public static int getSelectedVetId() {
        return selectedVetId;
    }

    public static void setSelectedVetId(int selectedVetId) {
        SessionManager.selectedVetId = selectedVetId;
    }

    public static String getSelectedVetNom() {
        return selectedVetNom == null ? "" : selectedVetNom;
    }

    public static void setSelectedVetNom(String selectedVetNom) {
        SessionManager.selectedVetNom = selectedVetNom;
    }

    public static void logout() {
        userId = 0;
        userNom = null;
        userRole = null;
        selectedVetId = 0;
        selectedVetNom = null;
    }
}
