package controllers;

import com.esprit.furhope.utils.AppSession;
import controllers.SessionContext;
import entities.ManagerAccount;
import entities.User;
import java.awt.Desktop;
import java.net.URI;
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
import javafx.scene.layout.VBox;
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
    private VBox ctaSection;

    @FXML
    private Label ctaTitleLabel;

    @FXML
    private Label ctaBodyLabel;

    @FXML
    private void initialize() {
        refreshUi();
        Platform.runLater(this::refreshUi);
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
    }

    private void setVisible(Button button, boolean visible) {
        if (button == null) {
            return;
        }
        button.setVisible(visible);
        button.setManaged(visible);
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
        openModule("Adoption");
    }

    @FXML
    private void openProducts(ActionEvent event) {
        openModule("Products");
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
        System.setProperty("furhope.api.base-url", "http://127.0.0.1:8081");
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
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene scene = new Scene(root);
            if ("/fxml/feed.fxml".equals(fxmlFile) || "/fxml/app.fxml".equals(fxmlFile)) {
                var socialCss = getClass().getResource("/css/app.css");
                if (socialCss != null) {
                    scene.getStylesheets().add(socialCss.toExternalForm());
                }
            }
            stage.setScene(scene);
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
