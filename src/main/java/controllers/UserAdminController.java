package com.esprit.controllers;

import entities.User;
import com.esprit.services.userservices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Locale;

public class UserAdminController {

    @FXML
    private ListView<User> userTable;

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

        userTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : formatUser(user));
            }
        });
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
        userTable.setItems(filtered);
    }

    @FXML
    private void activateSelected(ActionEvent event) {
        updateActive(true);
    }

    @FXML
    private void blockSelected(ActionEvent event) {
        updateActive(false);
    }

    private void updateActive(boolean active) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a user to update.");
            return;
        }
        if (isAdminUser(selected)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be modified.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && selected.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot modify your own active status.");
            return;
        }

        try {
            service.setActive(selected.getId(), active);
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Updated", "User status updated.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update user status.");
        }
    }

    @FXML
    private void deleteSelected(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a user to delete.");
            return;
        }
        if (isAdminUser(selected)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be deleted.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && selected.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot delete your own account.");
            return;
        }

        try {
            service.supprimer(selected.getId());
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

    private String formatUser(User user) {
        return String.format(
                "ID: %d | %s %s | %s | Role: %s | Active: %s",
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive() ? "Yes" : "No"
        );
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
                || containsIgnoreCase(user.getRole(), query)
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
}
