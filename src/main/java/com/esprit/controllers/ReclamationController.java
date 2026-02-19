package com.esprit.controllers;

import com.esprit.entities.Reclamation;
import com.esprit.entities.User;
import com.esprit.services.ReclamationService;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;

public class ReclamationController {

    @FXML
    private ListView<Reclamation> reclamationTable;

    @FXML
    private TextField sujetField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    private final ReclamationService service = new ReclamationService();
    private final ObservableList<Reclamation> sourceData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        reclamationTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation reclamation, boolean empty) {
                super.updateItem(reclamation, empty);
                setText(empty || reclamation == null ? null : formatReclamation(reclamation));
            }
        });

        statusCombo.setItems(FXCollections.observableArrayList("OPEN", "IN_PROGRESS", "RESOLVED"));
        statusCombo.getSelectionModel().select("OPEN");
        sortCombo.setItems(FXCollections.observableArrayList("Newest first", "Oldest first", "Status A-Z", "Sujet A-Z"));
        sortCombo.getSelectionModel().selectFirst();

        reclamationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> fillForm(selected));
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        refreshTable();

        if (!SessionContext.isAdmin()) {
            statusCombo.setDisable(true);
        }
    }

    @FXML
    private void addReclamation() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        if (sujetField.getText().trim().isEmpty() || descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Sujet and description are required.");
            return;
        }

        try {
            Reclamation reclamation = new Reclamation();
            reclamation.setClientId(user.getId());
            reclamation.setSujet(sujetField.getText().trim());
            reclamation.setDescription(descriptionArea.getText().trim());
            reclamation.setStatus("OPEN");
            service.ajouter(reclamation);
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add reclamation.");
        }
    }

    @FXML
    private void updateReclamation() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation to update.");
            return;
        }

        if (!canModify(selected)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only edit your own reclamations.");
            return;
        }

        try {
            selected.setSujet(sujetField.getText().trim());
            selected.setDescription(descriptionArea.getText().trim());
            if (SessionContext.isAdmin()) {
                selected.setStatus(statusCombo.getValue());
            }
            service.modifier(selected);
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update reclamation.");
        }
    }

    @FXML
    private void deleteReclamation() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation to delete.");
            return;
        }

        if (!canModify(selected)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only delete your own reclamations.");
            return;
        }

        try {
            service.supprimer(selected.getId());
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete reclamation.");
        }
    }

    @FXML
    private void openResponses(ActionEvent event) {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation first.");
            return;
        }
        SessionContext.setSelectedReclamationId(selected.getId());

        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.INFORMATION, "Admin Only", "Only admin can create responses.");
            return;
        }

        switchScene(event, "/reponse.fxml");
    }

    @FXML
    private void refreshTable() {
        try {
            if (SessionContext.isAdmin()) {
                sourceData.setAll(service.afficher());
            } else {
                sourceData.setAll(service.afficherParClient(SessionContext.getCurrentUser().getId()));
            }
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load reclamations.");
        }
    }

    @FXML
    private void applyFilters() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        ObservableList<Reclamation> filtered = FXCollections.observableArrayList(
                sourceData.filtered(rec -> matchesSearch(rec, query))
        );
        FXCollections.sort(filtered, buildComparator(sortCombo == null ? null : sortCombo.getValue()));
        reclamationTable.setItems(filtered);
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    private boolean canModify(Reclamation reclamation) {
        return SessionContext.isAdmin() || SessionContext.getCurrentUser().getId() == reclamation.getClientId();
    }

    private void fillForm(Reclamation reclamation) {
        if (reclamation == null) {
            return;
        }
        sujetField.setText(reclamation.getSujet());
        descriptionArea.setText(reclamation.getDescription());
        statusCombo.getSelectionModel().select(reclamation.getStatus());
    }

    private void clearForm() {
        sujetField.clear();
        descriptionArea.clear();
        statusCombo.getSelectionModel().select("OPEN");
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

    private String formatReclamation(Reclamation reclamation) {
        return String.format(
                "ID: %d | Client: %d | Sujet: %s | Status: %s | Created: %s",
                reclamation.getId(),
                reclamation.getClientId(),
                reclamation.getSujet(),
                reclamation.getStatus(),
                reclamation.getCreatedAt()
        );
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private boolean matchesSearch(Reclamation rec, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        return containsIgnoreCase(rec.getSujet(), query)
                || containsIgnoreCase(rec.getDescription(), query)
                || containsIgnoreCase(rec.getStatus(), query)
                || String.valueOf(rec.getClientId()).contains(query)
                || String.valueOf(rec.getId()).contains(query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<Reclamation> buildComparator(String selectedSort) {
        if ("Oldest first".equals(selectedSort)) {
            return Comparator.comparing(this::safeCreatedAt);
        }
        if ("Status A-Z".equals(selectedSort)) {
            return Comparator.comparing(rec -> safeString(rec.getStatus()));
        }
        if ("Sujet A-Z".equals(selectedSort)) {
            return Comparator.comparing(rec -> safeString(rec.getSujet()));
        }
        return Comparator.comparing(this::safeCreatedAt).reversed();
    }

    private LocalDateTime safeCreatedAt(Reclamation rec) {
        return rec.getCreatedAt() == null ? LocalDateTime.MIN : rec.getCreatedAt();
    }

    private String safeString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
