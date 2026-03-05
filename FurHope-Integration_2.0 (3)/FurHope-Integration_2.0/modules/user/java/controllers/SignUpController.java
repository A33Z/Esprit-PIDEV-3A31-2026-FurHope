package controllers;

import entities.User;
import integrations.auth.FaceAuthService;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import com.esprit.utils.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignUpController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField cityField;

    @FXML
    private ComboBox<String> roleCombo;

    @FXML
    private Button themeToggleButton;

    private final userservices service = new userservices();
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("CLIENT", "VETERINAIRE"));
        roleCombo.getSelectionModel().selectFirst();
        Platform.runLater(this::syncThemeToggleIcon);
    }

    @FXML
    private void signUp(ActionEvent event) {
        performSignUp(event, false);
    }

    @FXML
    private void signUpWithFaceId(ActionEvent event) {
        performSignUp(event, true);
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/Welcome.fxml");
    }

    @FXML
    private void goToSignIn(ActionEvent event) {
        switchToSignIn(event);
    }

    @FXML
    private void toggleDarkMode(ActionEvent event) {
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.toggle(scene);
        syncThemeToggleIcon();
    }

    private void switchToSignIn(ActionEvent event) {
        switchScene(event, "/signin.fxml");
    }

    private void performSignUp(ActionEvent event, boolean enrollFaceId) {
        if (!validateInputs()) {
            return;
        }

        String role = roleCombo.getSelectionModel().getSelectedItem();
        User user = new User(
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText().trim(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                cityField.getText().trim(),
                role
        );

        if ("VETERINAIRE".equals(role)) {
            user.setActive(false);
        }

        try {
            service.ajouter(user);
            String baseSuccessMessage = "VETERINAIRE".equals(role)
                    ? "Account created. Awaiting approval."
                    : "Account created.";

            if (enrollFaceId) {
                try {
                    faceAuthService.enroll(user.getEmail());
                    showAlert(Alert.AlertType.INFORMATION, "Success", baseSuccessMessage + " Face ID enrolled.");
                } catch (Exception faceError) {
                    faceError.printStackTrace();
                    showAlert(
                            Alert.AlertType.WARNING,
                            "Account Created",
                            baseSuccessMessage + " Face ID enrollment failed: " + faceError.getMessage()
                    );
                }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Success", baseSuccessMessage);
            }

            switchToSignIn(event);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Sign Up Failed", "Could not create account.");
        }
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open page.");
        }
    }

    private boolean validateInputs() {
        clearFieldErrors();

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()
                || confirmPassword.isEmpty() || phone.isEmpty() || address.isEmpty() || city.isEmpty()
                || roleCombo.getSelectionModel().isEmpty()) {
            markIfEmpty(firstNameField);
            markIfEmpty(lastNameField);
            markIfEmpty(emailField);
            markIfEmpty(passwordField);
            markIfEmpty(confirmPasswordField);
            markIfEmpty(phoneField);
            markIfEmpty(addressField);
            markIfEmpty(cityField);
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all fields.");
            return false;
        }

        if (!AuthValidation.isValidName(firstName) || !AuthValidation.isValidName(lastName)) {
            setInvalid(firstNameField);
            setInvalid(lastNameField);
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid first name or last name format.");
            return false;
        }

        if (!AuthValidation.isValidEmail(email)) {
            setInvalid(emailField);
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid email format.");
            return false;
        }

        if (!AuthValidation.isValidPhone(phone)) {
            setInvalid(phoneField);
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Phone must be 8 to 15 digits.");
            return false;
        }

        if (!AuthValidation.isStrongPassword(password)) {
            setInvalid(passwordField);
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Password must be at least 8 chars with uppercase, lowercase, and number.");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            setInvalid(confirmPasswordField);
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Password confirmation does not match.");
            return false;
        }

        try {
            if (service.existsByEmail(email)) {
                setInvalid(emailField);
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Email already exists.");
                return false;
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Unable to validate email uniqueness.");
            return false;
        }

        return true;
    }

    private void clearFieldErrors() {
        clearInvalid(firstNameField);
        clearInvalid(lastNameField);
        clearInvalid(emailField);
        clearInvalid(passwordField);
        clearInvalid(confirmPasswordField);
        clearInvalid(phoneField);
        clearInvalid(addressField);
        clearInvalid(cityField);
    }

    private void markIfEmpty(TextField field) {
        if (field != null && field.getText() != null && field.getText().trim().isEmpty()) {
            setInvalid(field);
        }
    }

    private void markIfEmpty(PasswordField field) {
        if (field != null && field.getText() != null && field.getText().trim().isEmpty()) {
            setInvalid(field);
        }
    }

    private void setInvalid(TextField field) {
        if (field != null && !field.getStyleClass().contains("invalid")) {
            field.getStyleClass().add("invalid");
        }
    }

    private void setInvalid(PasswordField field) {
        if (field != null && !field.getStyleClass().contains("invalid")) {
            field.getStyleClass().add("invalid");
        }
    }

    private void clearInvalid(TextField field) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
    }

    private void clearInvalid(PasswordField field) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void syncThemeToggleIcon() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isDarkModeEnabled() ? "\uD83C\uDF19" : "\u2600");
        }
    }
}
