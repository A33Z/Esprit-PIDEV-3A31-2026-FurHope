package controllers;

import entities.ManagerAccount;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Throwable t) {
            t.printStackTrace();
            String msg = t.getClass().getSimpleName() + ": " + extractRootMessage(t);
            if (msg.trim().isEmpty()) {
                msg = "Unable to open dashboard due to an unknown error.";
            }
            showError(msg);

            // Show detailed alert with stacktrace so developer can paste the error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error loading dashboard");
            alert.setHeaderText(msg);
            StringBuilder sb = new StringBuilder();
            Throwable cursor = t;
            while (cursor != null) {
                sb.append(cursor.toString()).append('\n');
                for (StackTraceElement el : cursor.getStackTrace()) {
                    sb.append("    at ").append(el.toString()).append('\n');
                }
                cursor = cursor.getCause();
                if (cursor != null) sb.append("Caused by:\n");
            }
            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(false);
            area.setPrefRowCount(18);
            area.setPrefColumnCount(80);
            alert.getDialogPane().setExpandableContent(area);
            alert.showAndWait();
        }
    }

    private String extractRootMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        return message.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}

