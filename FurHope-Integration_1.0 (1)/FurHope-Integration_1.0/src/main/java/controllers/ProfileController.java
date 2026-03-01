package controllers;

import entities.User;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ProfileController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField cityField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Button editProfileButton;

    @FXML
    private VBox detailsPane;

    private final userservices service = new userservices();
    private User currentUser;

    @FXML
    private void initialize() {
        currentUser = controllers.SessionContext.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please sign in to edit your profile.");
            return;
        }
        fillForm(currentUser);
        setDetailsVisible(false);
    }

    private void fillForm(User user) {
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        addressField.setText(user.getAddress());
        cityField.setText(user.getCity());
        if (profileNameLabel != null) {
            profileNameLabel.setText((safe(user.getFirstName()) + " " + safe(user.getLastName())).trim());
        }
        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText(buildInitials(user));
        }
    }

    @FXML
    private void saveProfile(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please sign in to edit your profile.");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        String newPassword = passwordField.getText().trim();
        String passwordToSave = newPassword.isEmpty() ? currentUser.getPassword() : newPassword;

        User updated = new User(
                currentUser.getId(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordToSave,
                phoneField.getText().trim(),
                addressField.getText().trim(),
                cityField.getText().trim(),
                currentUser.getRole(),
                currentUser.isActive(),
                currentUser.getCreatedAt()
        );

        try {
            service.modifier(updated);
            controllers.SessionContext.setCurrentUser(updated);
            currentUser = updated;
            clearPasswordFields();
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Your profile has been updated.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not update profile.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    @FXML
    private void toggleProfileDetails(ActionEvent event) {
        setDetailsVisible(detailsPane == null || !detailsPane.isVisible());
    }

    private boolean validateInputs() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()
                || address.isEmpty() || city.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill all required fields.");
            return false;
        }

        if (!AuthValidation.isValidName(firstName) || !AuthValidation.isValidName(lastName)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid first name or last name format.");
            return false;
        }

        if (!AuthValidation.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid email format.");
            return false;
        }

        if (!AuthValidation.isValidPhone(phone)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Phone must be 8 to 15 digits.");
            return false;
        }

        if (!password.isEmpty()) {
            if (!AuthValidation.isStrongPassword(password)) {
                showAlert(Alert.AlertType.WARNING, "Validation",
                        "Password must be at least 8 chars with uppercase, lowercase, and number.");
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Password confirmation does not match.");
                return false;
            }
        }

        try {
            if (service.existsByEmailExcludingId(email, currentUser.getId())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Email already exists.");
                return false;
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Unable to validate email uniqueness.");
            return false;
        }

        return true;
    }

    private void clearPasswordFields() {
        passwordField.clear();
        confirmPasswordField.clear();
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

    private String buildInitials(User user) {
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        String a = first.isEmpty() ? "" : first.substring(0, 1).toUpperCase();
        String b = last.isEmpty() ? "" : last.substring(0, 1).toUpperCase();
        String initials = (a + b).trim();
        return initials.isEmpty() ? "U" : initials;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void setDetailsVisible(boolean visible) {
        if (detailsPane != null) {
            detailsPane.setVisible(visible);
            detailsPane.setManaged(visible);
        }
        if (editProfileButton != null) {
            editProfileButton.setText(visible ? "Hide" : "✎");
        }
    }
}
