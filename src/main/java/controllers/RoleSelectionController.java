package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import security.SessionContext;

import java.io.IOException;

public class RoleSelectionController {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField userIdField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleContinueAsManager() {
        SessionContext.loginAsManager();
        navigateTo("/HotelManagerDashboard.fxml", "FurHope - Hotel Manager Dashboard");
    }

    @FXML
    private void handleContinueAsUser() {
        try {
            int userId = parseUserId(userIdField.getText());
            SessionContext.loginAsUser(userId);
            navigateTo("/UserDashboard.fxml", "FurHope - User Dashboard");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private int parseUserId(String rawValue) {
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
}
