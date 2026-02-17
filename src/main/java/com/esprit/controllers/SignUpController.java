package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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

    private final userservices service = new userservices();

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("CLIENT", "VETERINAIRE"));
        roleCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void signUp(ActionEvent event) {
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
            if ("VETERINAIRE".equals(role)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account created. Awaiting approval.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account created.");
            }
            switchToSignIn(event);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Sign Up Failed", "Could not create account.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/Welcome.fxml");
    }

    private void switchToSignIn(ActionEvent event) {
        switchScene(event, "/signin.fxml");
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
}
