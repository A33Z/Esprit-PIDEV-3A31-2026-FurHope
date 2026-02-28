package controllers;

import entities.User;
import com.esprit.services.auth.FaceAuthService;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignInController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label formErrorLabel;

    @FXML
    private Button themeToggleButton;

    private userservices service;
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    private void initialize() {
        try {
            service = new userservices();
        } catch (RuntimeException e) {
            service = null;
            setFormError("Database connection unavailable. Please start MySQL and retry.");
        }
        Platform.runLater(this::syncThemeToggleIcon);
    }

    @FXML
    private void login(ActionEvent event) {

        clearErrors();
        if (!validateCredentialsInputsInline()) {
            return;
        }
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
            return;
        }

        try {
            User user = service.login(
                    emailField.getText().trim(),
                    passwordField.getText().trim()
            );

            System.out.println("[LOGIN] id=" + user.getId()
                    + " email=" + user.getEmail()
                    + " firstName=" + user.getFirstName());

            SessionContext.setCurrentUser(user);
            loadAccueil(event);

        } catch (RuntimeException e) {

            if ("EMAIL_NOT_FOUND".equals(e.getMessage())) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
            } else if ("WRONG_PASSWORD".equals(e.getMessage())) {
                setFieldError(passwordField, passwordErrorLabel, "Incorrect password.");
            } else if ("ACCOUNT_INACTIVE".equals(e.getMessage())) {
                setFormError("Account awaiting approval.");
            } else {
                e.printStackTrace();
                setFormError("Login failed: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            setFormError("System error. Please try again.");
        }
    }

    @FXML
    private void loginWithGoogle(ActionEvent event) {
        setFormError("Google sign-in is not configured in this build.");
    }

    @FXML
    private void loginWithFace(ActionEvent event) {
        clearErrors();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required for face login.");
            return;
        }
        if (!AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            return;
        }
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
            return;
        }

        try {
            User user = service.findByEmail(email);
            if (user == null) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
                return;
            }
            if (!faceAuthService.hasEnrollment(email)) {
                setFormError("No face enrolled for this email. Use Enroll Face first.");
                return;
            }
            if (!faceAuthService.verify(email)) {
                setFormError("Face verification failed. Try again.");
                return;
            }
            if (!user.isActive() && !"VETERINAIRE".equalsIgnoreCase(user.getRole())) {
                setFormError("Account awaiting approval.");
                return;
            }

            SessionContext.setCurrentUser(user);
            loadAccueil(event);
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Face login failed: " + e.getMessage());
        }
    }

    @FXML
    private void enrollFace(ActionEvent event) {
        clearErrors();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required to enroll face.");
            return;
        }
        if (!AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            return;
        }
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
            return;
        }

        try {
            if (!service.existsByEmail(email)) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
                return;
            }
            faceAuthService.enroll(email);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Face enrolled successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Face enrollment failed: " + e.getMessage());
        }
    }

    private boolean validateCredentialsInputsInline() {
        boolean ok = true;

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required.");
            ok = false;
        }

        if (!email.isEmpty() && !AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            ok = false;
        }

        if (password.isEmpty()) {
            setFieldError(passwordField, passwordErrorLabel, "Password is required.");
            ok = false;
        }

        return ok;
    }

    private void loadAccueil(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                .getScene().getWindow();
        Scene newScene = new Scene(root);
        ThemeManager.applyToScene(newScene);
        stage.setScene(newScene);
        stage.show();
    }

    @FXML
    private void goBack(ActionEvent event) {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Welcome.fxml")
            );

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();

            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forgot_password.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Unable to open password recovery page.");
        }
    }

    @FXML
    private void toggleDarkMode(ActionEvent event) {
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.toggle(scene);
        syncThemeToggleIcon();
    }

    @FXML
    private void goToSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/signup.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Unable to open sign up page.");
        }
    }

    private void syncThemeToggleIcon() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isDarkModeEnabled() ? "\uD83C\uDF19" : "\u2600");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void clearErrors() {
        clearFieldError(emailField, emailErrorLabel);
        clearFieldError(passwordField, passwordErrorLabel);
        if (formErrorLabel != null) {
            formErrorLabel.setManaged(false);
            formErrorLabel.setVisible(false);
            formErrorLabel.setText("");
        }
    }

    private void setFormError(String message) {
        if (formErrorLabel == null) {
            showAlert(Alert.AlertType.ERROR, "Error", message);
            return;
        }
        formErrorLabel.setText(message);
        formErrorLabel.setManaged(true);
        formErrorLabel.setVisible(true);
    }

    private void setFieldError(TextField field, Label errorLabel, String message) {
        if (field != null) {
            if (!field.getStyleClass().contains("invalid")) {
                field.getStyleClass().add("invalid");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
        } else {
            setFormError(message);
        }
    }

    private void setFieldError(PasswordField field, Label errorLabel, String message) {
        if (field != null) {
            if (!field.getStyleClass().contains("invalid")) {
                field.getStyleClass().add("invalid");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
        } else {
            setFormError(message);
        }
    }

    private void clearFieldError(TextField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
        if (errorLabel != null) {
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }

    private void clearFieldError(PasswordField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
        if (errorLabel != null) {
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }
}

