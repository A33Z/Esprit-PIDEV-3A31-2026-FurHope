package controllers;

import entities.User;
import com.esprit.services.userservices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class AdminController {

    @FXML
    private FlowPane pendingCardsPane;

    private final userservices service = new userservices();

    @FXML
    private void initialize() {
        refreshTable();
    }

    @FXML
    private void approveUser(User selected) {
        try {
            service.approveUser(selected.getId());
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Approved", "User approved.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not approve user.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    @FXML
    private void refreshTable() {
        try {
            ObservableList<User> data = FXCollections.observableArrayList(service.getPendingVets());
            renderPendingCards(data);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load pending users.");
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

    private void renderPendingCards(List<User> users) {
        pendingCardsPane.getChildren().clear();
        if (users == null || users.isEmpty()) {
            Label empty = new Label("No pending veterinarian approvals.");
            empty.getStyleClass().add("card-subtitle");
            pendingCardsPane.getChildren().add(empty);
            return;
        }
        for (User user : users) {
            pendingCardsPane.getChildren().add(createPendingCard(user));
        }
    }

    private VBox createPendingCard(User user) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "user-card-block");
        card.setPadding(new Insets(14));
        card.setPrefWidth(330);

        Label name = new Label(displayName(user));
        name.getStyleClass().add("card-title");

        Label email = new Label(safeDisplay(user.getEmail(), "No email"));
        email.getStyleClass().add("card-subtitle");
        email.setWrapText(true);

        Label phone = new Label("Phone: " + safeDisplay(user.getPhone(), "-"));
        phone.getStyleClass().add("field-label");

        Label role = new Label("Role: " + safeDisplay(user.getRole(), "-"));
        role.getStyleClass().add("field-label");

        Label badge = new Label("PENDING");
        badge.getStyleClass().addAll("status-badge", "status-pending");

        Button approve = new Button("Approve");
        approve.getStyleClass().addAll("primary-button", "action-button-small");
        approve.setOnAction(e -> approveUser(user));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, badge, spacer, approve);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(name, email, phone, role, actions);
        return card;
    }

    private String displayName(User user) {
        String first = safeDisplay(user == null ? null : user.getFirstName(), "");
        String last = safeDisplay(user == null ? null : user.getLastName(), "");
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return "User #" + (user == null ? "-" : user.getId());
    }

    private String safeDisplay(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
