package com.esprit.controllers;

import com.esprit.entities.User;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Locale;

public class AdminController {

    @FXML
    private FlowPane vetCardsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private Label countLabel;

    private final userservices service = new userservices();
    private final ObservableList<User> sourceData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Admin access only.");
            return;
        }
        sortCombo.setItems(FXCollections.observableArrayList("Newest ID", "Oldest ID", "Name A-Z"));
        sortCombo.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        refreshTable();
    }

    @FXML
    private void refreshTable() {
        try {
            sourceData.setAll(service.getPendingVets());
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load pending users.");
        }
    }

    @FXML
    private void applyFilters() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        ObservableList<User> filtered = FXCollections.observableArrayList(
                sourceData.filtered(user -> matchesSearch(user, query))
        );
        FXCollections.sort(filtered, buildComparator(sortCombo == null ? null : sortCombo.getValue()));
        renderCards(filtered);
        updateCount(filtered.size());
    }

    private void renderCards(ObservableList<User> users) {
        vetCardsContainer.getChildren().clear();
        for (User user : users) {
            vetCardsContainer.getChildren().add(createVetCard(user));
        }
    }

    private VBox createVetCard(User user) {
        Label title = new Label(buildDisplayName(user));
        Label role = new Label("Role: " + safe(user.getRole()));
        Label email = new Label("Email: " + safe(user.getEmail()));
        Label phone = new Label("Phone: " + safe(user.getPhone()));
        Label location = new Label("Address: " + safe(user.getAddress()) + ", " + safe(user.getCity()));
        Label pending = new Label("Status: Pending approval");

        title.getStyleClass().add("card-title");
        role.getStyleClass().add("card-subtitle");
        email.getStyleClass().add("card-subtitle");
        phone.getStyleClass().add("card-subtitle");
        location.getStyleClass().add("card-subtitle");
        pending.getStyleClass().add("card-subtitle");
        pending.setStyle("-fx-text-fill: #b35a00; -fx-font-weight: bold;");

        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().addAll("primary-button", "action-button");
        approveButton.setOnAction(e -> approveUser(user));

        HBox actions = new HBox(10, approveButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, title, role, email, phone, location, pending, actions);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(330);
        card.setMaxWidth(330);
        return card;
    }

    private void approveUser(User user) {
        if (user == null) {
            return;
        }
        try {
            service.approveUser(user.getId());
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Approved", "User approved: " + safe(user.getRole()));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not approve user.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
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

    private boolean matchesSearch(User user, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(user.getFirstName(), query)
                || containsIgnoreCase(user.getLastName(), query)
                || containsIgnoreCase(user.getEmail(), query)
                || containsIgnoreCase(user.getPhone(), query)
                || containsIgnoreCase(user.getCity(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<User> buildComparator(String selectedSort) {
        if ("Oldest ID".equals(selectedSort)) {
            return Comparator.comparingInt(User::getId);
        }
        if ("Name A-Z".equals(selectedSort)) {
            return Comparator.comparing(this::fullNameKey);
        }
        return Comparator.comparingInt(User::getId).reversed();
    }

    private String fullNameKey(User user) {
        return safeString(user.getFirstName()) + " " + safeString(user.getLastName());
    }

    private String safeString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private String buildDisplayName(User user) {
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        if ("-".equals(first) && "-".equals(last)) {
            return "Unnamed User";
        }
        if ("-".equals(first)) {
            return last;
        }
        if ("-".equals(last)) {
            return first;
        }
        return first + " " + last;
    }

    private void updateCount(int count) {
        if (countLabel != null) {
            if (count == 0) {
                countLabel.setText("No pending approvals right now.");
            } else if (count == 1) {
                countLabel.setText("1 user pending approval.");
            } else {
                countLabel.setText(count + " users pending approval.");
            }
        }
    }
}
