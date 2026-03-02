package utils;

public class SessionManager {

    private static int userId;
    private static String userNom;
    private static String userRole;

    // ✅ Pour stocker le vétérinaire choisi par le client
    private static int selectedVetId;
    private static String selectedVetNom;

    public static void setUserId(int id) { userId = id; }
    public static int getUserId() { return userId; }

    public static void setUserNom(String nom) { userNom = nom; }
    public static String getUserNom() { return userNom; }

    public static void setUserRole(String role) { userRole = role; }
    public static String getUserRole() { return userRole; }

    public static boolean isVeterinaire() {
        return "VETERINAIRE".equals(userRole);
    }

    // ✅ Vétérinaire sélectionné
    public static void setSelectedVetId(int id) { selectedVetId = id; }
    public static int getSelectedVetId() { return selectedVetId; }

    public static void setSelectedVetNom(String nom) { selectedVetNom = nom; }
    public static String getSelectedVetNom() { return selectedVetNom; }

    public static void logout() {
        userId = 0;
        userNom = null;
        userRole = null;
        selectedVetId = 0;
        selectedVetNom = null;
    }
}