package controllers;

import com.esprit.furhope.utils.AppSession;
import com.esprit.animal.Services.AnimalSchemaBootstrapService;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.utils.ThemeManager;
import controllers.SessionContext;
import entities.ManagerAccount;
import entities.User;
import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import utils.SessionManager;

public class AccueilController {

    @FXML
    private Button signInButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button profileButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Label welcomeLabel;

    @FXML
    private ImageView profileAvatarImageView;

    @FXML
    private StackPane profileAvatarFallback;

    @FXML
    private VBox ctaSection;

    @FXML
    private Label ctaTitleLabel;

    @FXML
    private Label ctaBodyLabel;

    @FXML
    private void initialize() {
        refreshUi();
        Platform.runLater(this::refreshUi);
        if (profileAvatarImageView != null) {
            profileAvatarImageView.setClip(new Circle(24, 24, 24));
        }
        if (signInButton != null) {
            signInButton.sceneProperty().addListener((obs, oldScene, newScene) -> refreshUi());
        }
    }

    private void refreshUi() {
        User user = SessionContext.getCurrentUser();
        boolean loggedIn = user != null;

        setVisible(signInButton, !loggedIn);
        setVisible(signUpButton, !loggedIn);

        setVisible(dashboardButton, loggedIn);
        setVisible(profileButton, loggedIn);
        setVisible(logoutButton, loggedIn);
        if (ctaSection != null) {
            ctaSection.setVisible(true);
            ctaSection.setManaged(true);
        }
        if (ctaTitleLabel != null && ctaBodyLabel != null) {
            if (loggedIn) {
                ctaTitleLabel.setText("Welcome back!");
                ctaBodyLabel.setText("You are signed in. Explore the dashboard to manage your work and follow animals.");
            } else {
                ctaTitleLabel.setText("Ready to help?");
                ctaBodyLabel.setText("Sign in to adopt animals, follow their stories, and access everything FurHope offers.");
            }
        }

        if (welcomeLabel != null) {
            if (loggedIn) {
                welcomeLabel.setText("Welcome back, " + resolveDisplayName(user));
            } else {
                welcomeLabel.setText("Welcome to FurHope");
            }
        }
        updateHeaderAvatar(user);
    }

    private void setVisible(Button button, boolean visible) {
        if (button == null) {
            return;
        }
        button.setVisible(visible);
        button.setManaged(visible);
    }

    private void updateHeaderAvatar(User user) {
        if (profileAvatarImageView == null) {
            return;
        }
        Image avatarImage = user == null ? null : loadAvatarImage(user.getProfileImagePath());
        boolean hasAvatar = avatarImage != null && !avatarImage.isError();

        profileAvatarImageView.setImage(hasAvatar ? avatarImage : null);
        profileAvatarImageView.setVisible(hasAvatar);
        profileAvatarImageView.setManaged(hasAvatar);
        if (profileAvatarFallback != null) {
            profileAvatarFallback.setVisible(!hasAvatar);
            profileAvatarFallback.setManaged(!hasAvatar);
        }
    }

    private Image loadAvatarImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String trimmedPath = imagePath.trim();
        try {
            if (trimmedPath.startsWith("http://")
                    || trimmedPath.startsWith("https://")
                    || trimmedPath.startsWith("file:")) {
                return new Image(trimmedPath, false);
            }
            java.io.File file = new java.io.File(trimmedPath);
            if (!file.exists()) {
                return null;
            }
            return new Image(file.toURI().toString(), false);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "Friend";
        }
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        if (!firstName.isEmpty()) {
            return firstName;
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (!email.isEmpty()) {
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        return "Friend";
    }

    @FXML
    private void goToSignIn(ActionEvent event) {
        switchScene(event, "/signin.fxml");
    }

    @FXML
    private void goToSignUp(ActionEvent event) {
        switchScene(event, "/signup.fxml");
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please sign in first.");
            return;
        }
        switchScene(event, "/dashboard.fxml");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please sign in first.");
            return;
        }
        switchScene(event, "/profile.fxml");
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionContext.clear();
        SessionManager.logout();
        switchScene(event, "/accueil.fxml");
    }

    @FXML
    private void openAdoption(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access Adoption.");
            return;
        }

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Session Error", "Unable to resolve current user session.");
            return;
        }

        try {
            AnimalSchemaBootstrapService.ensureSchemaReady();
            bootstrapAnimalSession(user);
            switchScene(event, "/animal/AfficherAnimal.fxml");
        } catch (RuntimeException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Adoption Error", "Unable to open Adoption module.");
        }
    }

    @FXML
    private void openProducts(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access Products.");
            return;
        }

        try {
            switchScene(event, "/shop.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Products Error", "Unable to open Products module.");
        }
    }

    @FXML
    private void openVetCare(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access Vet Care.");
            return;
        }

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Session Error", "Unable to resolve current user session.");
            return;
        }

        syncLegacyDashboardSession(user);
        if (isVeterinarian(user)) {
            switchScene(event, "/VetDashboard.fxml");
        } else {
            switchScene(event, "/DashboardClient.fxml");
        }
    }

    @FXML
    private void openSocial(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access Social.");
            return;
        }

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Session Error", "Unable to resolve current user session.");
            return;
        }

        int userId = user.getId() > 0 ? user.getId() : 1;
        AppSession.setCurrentUser(userId, resolveDisplayName(user));
        switchScene(event, "/fxml/app.fxml");
    }

    @FXML
    private void openHostel(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access Hostel.");
            return;
        }

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Session Error", "Unable to resolve current user session.");
            return;
        }

        try {
            if (isAdminOrManager(user)) {
                String managerId = user.getManagerId();
                if (managerId == null || managerId.trim().isEmpty()) {
                    managerId = "ADMIN-" + Math.max(user.getId(), 1);
                }
                services.SessionContext.startManagerSession(
                        new ManagerAccount(managerId, resolveDisplayName(user))
                );
                switchScene(event, "/HotelManagerDashboard.fxml");
            } else {
                int userId = user.getId() > 0 ? user.getId() : 1;
                services.SessionContext.startUserSession(userId);
                switchScene(event, "/UserDashboard.fxml");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Access Error", "Unable to open Hostel module.");
        }
    }

    private boolean isAdminOrManager(User user) {
        if (user == null) {
            return false;
        }
        String role = user.getRole();
        if (role == null) {
            return false;
        }
        String normalizedRole = role.trim();
        return "ADMIN".equalsIgnoreCase(normalizedRole)
                || "HOTEL_MANAGER".equalsIgnoreCase(normalizedRole)
                || "MANAGER".equalsIgnoreCase(normalizedRole);
    }

    private boolean isVeterinarian(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String normalizedRole = user.getRole()
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return "VETERINAIRE".equals(normalizedRole)
                || "VETERINARIAN".equals(normalizedRole)
                || "VET".equals(normalizedRole);
    }

    private void syncLegacyDashboardSession(User user) {
        SessionManager.setUserId(user.getId());
        SessionManager.setUserNom(resolveDisplayName(user));
        SessionManager.setUserRole(user.getRole());
    }

    private void bootstrapAnimalSession(User user) {
        int fallbackUserId = Math.max(user.getId(), 1);
        String displayName = resolveDisplayName(user);

        com.esprit.animal.utils.Session.setUserId(fallbackUserId);
        com.esprit.animal.utils.Session.setUserName(displayName);
        com.esprit.animal.utils.Session.setUserEmail(user.getEmail());
        com.esprit.animal.utils.Session.setUserRole(user.getRole());
        com.esprit.animal.utils.Session.setUserPhone(parsePhoneAsInt(user.getPhone()));

        int compteId = resolveAnimalCompteId(user, fallbackUserId);
        com.esprit.animal.utils.Session.setCompteId(compteId > 0 ? compteId : fallbackUserId);
    }

    private int resolveAnimalCompteId(User user, int fallbackUserId) {
        try {
            Connection connection = com.esprit.animal.utils.MyDataBase.getInstance().getConnection();
            if (connection == null || connection.isClosed()) {
                return fallbackUserId;
            }

            Integer compteId = null;
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                for (String emailQuery : new String[]{
                        "SELECT c.id_compte FROM compte c JOIN user u ON c.user_id = u.id_user WHERE u.email = ? LIMIT 1",
                        "SELECT c.id_compte FROM compte c JOIN user u ON c.user_id = u.id WHERE u.email = ? LIMIT 1"
                }) {
                    compteId = querySingleInt(connection, emailQuery, user.getEmail());
                    if (compteId != null && compteId > 0) {
                        return compteId;
                    }
                }
            }

            for (String userIdQuery : new String[]{
                    "SELECT id_compte FROM compte WHERE user_id = ? LIMIT 1"
            }) {
                compteId = querySingleInt(connection, userIdQuery, fallbackUserId);
                if (compteId != null && compteId > 0) {
                    return compteId;
                }
            }

            Integer createdCompteId = createAnimalCompteIfMissing(connection, user, fallbackUserId);
            if (createdCompteId != null && createdCompteId > 0) {
                return createdCompteId;
            }
        } catch (SQLException ignored) {
            return fallbackUserId;
        }

        return fallbackUserId;
    }

    private Integer createAnimalCompteIfMissing(Connection connection, User user, int fallbackUserId) {
        String username = resolveAnimalUsername(user, fallbackUserId);
        String role = normalizeAnimalCompteRole(user == null ? null : user.getRole());

        for (String sql : new String[]{
                "INSERT INTO compte (user_id, username, password, role, status) VALUES (?, ?, '', ?, 'ACTIVE')",
                "INSERT INTO compte (user_id, username, password, role) VALUES (?, ?, '', ?)",
                "INSERT INTO compte (user_id, username, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                "INSERT INTO compte (user_id, username, role) VALUES (?, ?, ?)"
        }) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, fallbackUserId);
                statement.setString(2, username);
                statement.setString(3, role);
                statement.executeUpdate();

                Integer compteId = querySingleInt(connection,
                        "SELECT id_compte FROM compte WHERE user_id = ? ORDER BY id_compte DESC LIMIT 1",
                        fallbackUserId);
                if (compteId != null && compteId > 0) {
                    return compteId;
                }
            } catch (SQLException ignored) {
                // Try next insert variant if table definition differs.
            }
        }
        return null;
    }

    private String resolveAnimalUsername(User user, int fallbackUserId) {
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim();
        }
        String displayName = user == null ? null : resolveDisplayName(user);
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim().replaceAll("\\s+", "_").toLowerCase(Locale.ROOT);
        }
        return "user_" + fallbackUserId;
    }

    private String normalizeAnimalCompteRole(String role) {
        if (role == null || role.isBlank()) {
            return "CLIENT";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("ADMIN")) {
            return "ADMIN";
        }
        if (normalized.contains("VET")) {
            return "VET";
        }
        if (normalized.contains("HOTEL") || normalized.contains("MANAGER")) {
            return "MANAGER";
        }
        return "CLIENT";
    }

    private Integer querySingleInt(Connection connection, String sql, Object parameter) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter instanceof Integer) {
                statement.setInt(1, (Integer) parameter);
            } else {
                statement.setString(1, parameter == null ? null : parameter.toString());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int value = resultSet.getInt(1);
                    return resultSet.wasNull() ? null : value;
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private int parsePhoneAsInt(String phoneRaw) {
        if (phoneRaw == null || phoneRaw.isBlank()) {
            return 0;
        }
        String digits = phoneRaw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        if (digits.length() > 9) {
            digits = digits.substring(digits.length() - 9);
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void openModule(String name) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required",
                    "Please sign in to access " + name + ".");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon",
                name + " will be linked by your teammate.");
    }

    @FXML
    private void openDonate(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://furhope.example/donate"));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.INFORMATION, "Donate", "Please contact the shelter to donate.");
        }
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root;
            if (fxmlFile != null && fxmlFile.startsWith("/animal/")) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile), LanguageManager.getBundle());
                root = loader.load();
            } else {
                root = FXMLLoader.load(getClass().getResource(fxmlFile));
            }
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene scene = new Scene(root);
            if ("/fxml/feed.fxml".equals(fxmlFile) || "/fxml/app.fxml".equals(fxmlFile)) {
                var socialCss = getClass().getResource("/css/app.css");
                if (socialCss != null) {
                    scene.getStylesheets().add(socialCss.toExternalForm());
                }
            }
            ThemeManager.applyToScene(scene);
            stage.setScene(scene);
            ThemeManager.applyToStage(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open page.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
