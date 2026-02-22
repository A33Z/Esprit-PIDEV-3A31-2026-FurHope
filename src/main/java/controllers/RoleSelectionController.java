package controllers;

import entities.ManagerAccount;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import services.ManagerAuthService;
import services.SessionContext;

import java.io.IOException;
import java.util.Optional;

public class RoleSelectionController {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField managerIdField;
    @FXML
    private PasswordField managerPasswordField;
    @FXML
    private TextField userIdField;
    @FXML
    private Label errorLabel;

    private ManagerAuthService managerAuthService;

    @FXML
    public void initialize() {
        managerAuthService = new ManagerAuthService();
    }

    @FXML
    private void handleManagerLogin() {
        clearError();

        String managerId = managerIdField.getText();
        if (managerId == null || managerId.trim().isEmpty()) {
            showError("Manager ID is required.");
            return;
        }

        char[] password = managerPasswordField.getText() == null
                ? new char[0]
                : managerPasswordField.getText().toCharArray();

        try {
            Optional<ManagerAccount> account = managerAuthService.authenticate(managerId, password);
            if (account.isEmpty()) {
                showError("Invalid manager credentials.");
                return;
            }
            SessionContext.startManagerSession(account.get());
            navigateTo("/HotelManagerDashboard.fxml", "FurHope - Hotel Manager Dashboard");
        } catch (RuntimeException e) {
            showError("Authentication service unavailable.");
        } finally {
            managerPasswordField.clear();
        }
    }

    @FXML
    private void handleContinueAsUser() {
        clearError();
        try {
            int userId = resolveUserId(userIdField.getText());
            SessionContext.startUserSession(userId);
            navigateTo("/UserDashboard.fxml", "FurHope - User Dashboard");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private int resolveUserId(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required.");
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("User ID must be > 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("User ID must be numeric.");
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            showError("Unable to open dashboard.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
