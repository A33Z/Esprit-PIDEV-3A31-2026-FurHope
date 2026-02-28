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

public class UserAdminController {

    @FXML
    private FlowPane userCardsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    private final userservices service = new userservices();
    private final ObservableList<User> sourceData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can manage users.");
            return;
        }

        sortCombo.setItems(FXCollections.observableArrayList("Newest ID", "Oldest ID", "Name A-Z", "Role A-Z"));
        sortCombo.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        try {
            sourceData.setAll(service.afficher());
            sourceData.removeIf(this::isAdminUser);
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load users.");
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
    }

    private void renderCards(ObservableList<User> users) {
        userCardsContainer.getChildren().clear();
        for (User user : users) {
            userCardsContainer.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(User user) {
        Label nameLabel = new Label(buildDisplayName(user));
        Label emailLabel = new Label("Email: " + safe(user.getEmail()));
        Label phoneLabel = new Label("Phone: " + safe(user.getPhone()));
        Label locationLabel = new Label("Address: " + safe(user.getAddress()) + ", " + safe(user.getCity()));
        Label statusLabel = new Label("Account status: " + (user.isActive() ? "Active" : "Blocked"));

        nameLabel.getStyleClass().add("card-title");
        emailLabel.getStyleClass().add("card-subtitle");
        phoneLabel.getStyleClass().add("card-subtitle");
        locationLabel.getStyleClass().add("card-subtitle");
        statusLabel.getStyleClass().add("card-subtitle");

        Button activateButton = new Button("Activate");
        Button blockButton = new Button("Block");
        Button deleteButton = new Button("Delete");

        activateButton.getStyleClass().addAll("primary-button", "action-button");
        blockButton.getStyleClass().addAll("secondary-button", "action-button");
        deleteButton.getStyleClass().addAll("ghost-button", "action-button");

        activateButton.setDisable(user.isActive());
        blockButton.setDisable(!user.isActive());

        activateButton.setOnAction(e -> updateActive(user, true));
        blockButton.setOnAction(e -> updateActive(user, false));
        deleteButton.setOnAction(e -> deleteUser(user));

        HBox actions = new HBox(10, activateButton, blockButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, nameLabel, emailLabel, phoneLabel, locationLabel, statusLabel, actions);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        return card;
    }

    private void updateActive(User user, boolean active) {
        if (user == null) {
            return;
        }
        if (isAdminUser(user)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be modified.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && user.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot modify your own active status.");
            return;
        }

        try {
            service.setActive(user.getId(), active);
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Updated", "User status updated.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update user status.");
        }
    }

    private void deleteUser(User user) {
        if (user == null) {
            return;
        }
        if (isAdminUser(user)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be deleted.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && user.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot delete your own account.");
            return;
        }

        try {
            service.supprimer(user.getId());
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Deleted", "User deleted.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete user.");
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

    private boolean isAdminUser(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().trim());
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
                || containsIgnoreCase(user.getCity(), query)
                || String.valueOf(user.getId()).contains(query);
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
        if ("Role A-Z".equals(selectedSort)) {
            return Comparator.comparing(user -> safeString(user.getRole()));
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
}
