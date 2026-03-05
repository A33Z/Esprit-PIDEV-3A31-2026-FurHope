package controllers;

import com.esprit.furhope.utils.AppSession;
import controllers.SessionContext;
import entities.User;
import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button adminButton;

    @FXML
    private Button reclamationButton;

    @FXML
    private Button usersButton;

    @FXML
    private Button profileButton;

    private User currentUser;

    @FXML
    private void initialize() {
        // Default to session user if this controller was opened without an explicit setUser(...) call.
        currentUser = SessionContext.getCurrentUser();
        refreshUi();
    }

    public void setUser(User user) {
        SessionContext.setCurrentUser(user);
        this.currentUser = user;
        refreshUi();
    }

    private void refreshUi() {
        // Do not overwrite the explicitly provided user. Fall back to session only if needed.
        if (currentUser == null) {
            currentUser = SessionContext.getCurrentUser();
        }

        if (currentUser == null) {
            welcomeLabel.setText("Welcome, Guest");
            adminButton.setVisible(false);
            adminButton.setManaged(false);
            if (usersButton != null) {
                usersButton.setVisible(false);
                usersButton.setManaged(false);
            }
            if (profileButton != null) {
                profileButton.setVisible(false);
                profileButton.setManaged(false);
            }
            reclamationButton.setText("Login to access Reclamation");
            return;
        }

        welcomeLabel.setText("Welcome, " + resolveDisplayName(currentUser));
        boolean isAdmin = SessionContext.isAdmin();
        adminButton.setVisible(isAdmin);
        adminButton.setManaged(isAdmin);
        if (usersButton != null) {
            usersButton.setVisible(isAdmin);
            usersButton.setManaged(isAdmin);
        }
        if (profileButton != null) {
            profileButton.setVisible(true);
            profileButton.setManaged(true);
        }
        reclamationButton.setText("Client Reclamation");
    }

    private String resolveDisplayName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        if (!firstName.isEmpty()) {
            return firstName;
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (!email.isEmpty()) {
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        return "User";
    }

    @FXML
    private void openAdoption() {
        openProtectedPlaceholder("Adoption page will be linked by your teammate.");
    }

    @FXML
    private void openProduct() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login first.");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/shop.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            ThemeManager.applyToScene(scene);
            stage.setScene(scene);
            ThemeManager.applyToStage(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open Products page.");
        }
    }

    @FXML
    private void openVetRdv() {
        openProtectedPlaceholder("Veterinaire rendez-vous page will be linked by your teammate.");
    }

    @FXML
    private void openSocial() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login first.");
            return;
        }
        try {
            User user = SessionContext.getCurrentUser();
            if (user == null) {
                showAlert(Alert.AlertType.WARNING, "Session Error", "Unable to resolve current user.");
                return;
            }
            int userId = user.getId() > 0 ? user.getId() : 1;
            AppSession.setCurrentUser(userId, resolveDisplayName(user));
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/app.fxml"));
            Scene scene = new Scene(root);
            var socialCss = getClass().getResource("/css/app.css");
            if (socialCss != null) {
                scene.getStylesheets().add(socialCss.toExternalForm());
            }
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open Social page.");
        }
    }

    @FXML
    private void openHostelLocation() {
        openProtectedPlaceholder("Hostel location page will be linked by your teammate.");
    }

    @FXML
    private void openReclamation(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please sign in to create and follow reclamations.");
            return;
        }
        switchScene(event, "/reclamation.fxml");
    }

    @FXML
    private void goToAdmin(ActionEvent event) {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Admin access only.");
            return;
        }
        switchScene(event, "/admin.fxml");
    }

    @FXML
    private void goToUserAdmin(ActionEvent event) {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Admin access only.");
            return;
        }
        switchScene(event, "/user_admin.fxml");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please sign in to access your profile.");
            return;
        }
        switchScene(event, "/profile.fxml");
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionContext.clear();
        switchScene(event, "/accueil.fxml");
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/accueil.fxml");
    }

    private void openProtectedPlaceholder(String message) {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login first.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon", message);
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene scene = new Scene(root);
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
