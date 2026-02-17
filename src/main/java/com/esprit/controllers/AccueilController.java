package com.esprit.controllers;

import com.esprit.entities.User;
import java.awt.Desktop;
import java.net.URI;
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
        openModule("Vet Care");
    }

    @FXML
    private void openSocial(ActionEvent event) {
        openModule("Social");
    }

    @FXML
    private void openHostel(ActionEvent event) {
        openModule("Hostel");
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
            stage.setScene(new Scene(root));
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
