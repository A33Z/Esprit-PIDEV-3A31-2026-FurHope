package com.esprit.controllers;

import com.esprit.entities.Reponse;
import com.esprit.services.ReponseService;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ReponseController {

    @FXML
    private ListView<Reponse> reponseTable;

    @FXML
    private TextField reclamationIdField;
    @FXML
    private TextArea messageArea;

    private final ReponseService service = new ReponseService();

    @FXML
    private void initialize() {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can manage responses.");
            return;
        }

        reponseTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reponse reponse, boolean empty) {
                super.updateItem(reponse, empty);
                setText(empty || reponse == null ? null : formatReponse(reponse));
            }
        });

        Integer selectedReclamation = SessionContext.getSelectedReclamationId();
        if (selectedReclamation != null) {
            reclamationIdField.setText(String.valueOf(selectedReclamation));
            loadByReclamation(selectedReclamation);
        } else {
            refreshTable();
        }

        reponseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> fillForm(selected));
    }

    @FXML
    private void addReponse() {
        if (!validate()) {
            return;
        }

        try {
            Reponse reponse = new Reponse();
            reponse.setReclamationId(Integer.parseInt(reclamationIdField.getText().trim()));
            reponse.setAdminId(SessionContext.getCurrentUser().getId());
            reponse.setMessage(messageArea.getText().trim());
            service.ajouter(reponse);
            clearForm();
            refreshTable();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add response.");
        }
    }

    @FXML
    private void updateReponse() {
        Reponse selected = reponseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a response to update.");
            return;
        }
        if (!validate()) {
            return;
        }

        try {
            selected.setMessage(messageArea.getText().trim());
            service.modifier(selected);
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update response.");
        }
    }

    @FXML
    private void deleteReponse() {
        Reponse selected = reponseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a response to delete.");
            return;
        }

        try {
            service.supprimer(selected.getId());
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete response.");
        }
    }

    @FXML
    private void refreshTable() {
        try {
            String value = reclamationIdField.getText() == null ? "" : reclamationIdField.getText().trim();
            if (!value.isEmpty()) {
                loadByReclamation(Integer.parseInt(value));
            } else {
                ObservableList<Reponse> data = FXCollections.observableArrayList(service.afficher());
                reponseTable.setItems(data);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load responses.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/reclamation.fxml");
    }

    private void loadByReclamation(int reclamationId) {
        try {
            ObservableList<Reponse> data = FXCollections.observableArrayList(service.afficherParReclamation(reclamationId));
            reponseTable.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load response list.");
        }
    }

    private boolean validate() {
        if (reclamationIdField.getText().trim().isEmpty() || messageArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID and message are required.");
            return false;
        }
        return true;
    }

    private void fillForm(Reponse reponse) {
        if (reponse == null) {
            return;
        }
        reclamationIdField.setText(String.valueOf(reponse.getReclamationId()));
        messageArea.setText(reponse.getMessage());
    }

    private void clearForm() {
        messageArea.clear();
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open page.");
        }
    }

    private String formatReponse(Reponse reponse) {
        return String.format(
                "ID: %d | Reclamation: %d | Admin: %d | Message: %s | Created: %s",
                reponse.getId(),
                reponse.getReclamationId(),
                reponse.getAdminId(),
                reponse.getMessage(),
                reponse.getCreatedAt()
        );
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
