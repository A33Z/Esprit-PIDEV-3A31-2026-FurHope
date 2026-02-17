package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.services.userservices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class UserAdminController {

    @FXML
    private ListView<User> userTable;

    private final userservices service = new userservices();

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

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        try {
            ObservableList<User> data = FXCollections.observableArrayList(service.afficher());
            data.removeIf(this::isAdminUser);
            userTable.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load users.");
        }
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
}
