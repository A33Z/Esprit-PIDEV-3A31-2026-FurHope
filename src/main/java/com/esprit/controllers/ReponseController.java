package com.esprit.controllers;

import com.esprit.entities.Reclamation;
import com.esprit.entities.Reponse;
import com.esprit.entities.User;
import com.esprit.services.ReclamationService;
import com.esprit.services.ReponseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReponseController {

    @FXML
    private Label titleLabel;

    @FXML
    private ListView<Reponse> reponseTable;

    @FXML
    private TextField reclamationIdField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button approveButton;

    private final ReponseService service = new ReponseService();
    private final ReclamationService reclamationService = new ReclamationService();
    private boolean readOnlyClient;
    private static final String AI_DRAFT_PREFIX = "[AI DRAFT - REVIEW REQUIRED]";
    private static final String REVIEWED_PREFIX = "[REVIEWED]";

    @FXML
    private void initialize() {
        readOnlyClient = !SessionContext.isAdmin();

        reponseTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reponse reponse, boolean empty) {
                super.updateItem(reponse, empty);
                if (empty || reponse == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(null);
                setGraphic(createResponseCard(reponse));
                String msg = reponse.getMessage() == null ? "" : reponse.getMessage().trim();
                if (msg.startsWith(AI_DRAFT_PREFIX)) {
                    setStyle("-fx-background-color: rgba(255, 173, 51, 0.18); -fx-background-radius: 10;");
                } else if (msg.startsWith(REVIEWED_PREFIX)) {
                    setStyle("-fx-background-color: rgba(66, 153, 225, 0.14); -fx-background-radius: 10;");
                } else {
                    setStyle("");
                }
            }
        });

        Integer selectedReclamation = SessionContext.getSelectedReclamationId();
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation first.");
            return;
        }

        if (!canAccessReclamation(selectedReclamation)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot access this reclamation.");
            return;
        }

        reclamationIdField.setText(String.valueOf(selectedReclamation));
        loadByReclamation(selectedReclamation);

        if (readOnlyClient) {
            applyClientReadOnlyMode();
        } else {
            reponseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> fillForm(selected));
        }
    }

    @FXML
    private void addReponse() {
        if (readOnlyClient) {
            showAlert(Alert.AlertType.INFORMATION, "Read Only", "Clients can only view responses.");
            return;
        }
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
        if (readOnlyClient) {
            showAlert(Alert.AlertType.INFORMATION, "Read Only", "Clients can only view responses.");
            return;
        }
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
        if (readOnlyClient) {
            showAlert(Alert.AlertType.INFORMATION, "Read Only", "Clients can only view responses.");
            return;
        }
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
    private void approveAiDraft() {
        if (readOnlyClient) {
            showAlert(Alert.AlertType.INFORMATION, "Read Only", "Clients can only view responses.");
            return;
        }
        Reponse selected = reponseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select an AI draft to approve.");
            return;
        }
        String message = selected.getMessage() == null ? "" : selected.getMessage().trim();
        if (!message.startsWith(AI_DRAFT_PREFIX)) {
            showAlert(Alert.AlertType.INFORMATION, "Not Draft", "Selected response is not an AI draft.");
            return;
        }
        String cleaned = message.substring(AI_DRAFT_PREFIX.length()).trim();
        if (cleaned.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Draft", "Draft content is empty.");
            return;
        }
        try {
            selected.setMessage(REVIEWED_PREFIX + "\n" + cleaned);
            service.modifier(selected);
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to approve AI draft.");
        }
    }

    @FXML
    private void refreshTable() {
        try {
            String value = reclamationIdField.getText() == null ? "" : reclamationIdField.getText().trim();
            if (value.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID is required.");
                return;
            }

            int reclamationId = Integer.parseInt(value);
            if (!canAccessReclamation(reclamationId)) {
                showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot access this reclamation.");
                reponseTable.setItems(FXCollections.observableArrayList());
                return;
            }
            loadByReclamation(reclamationId);
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

    private void applyClientReadOnlyMode() {
        if (titleLabel != null) {
            titleLabel.setText("Responses");
        }
        if (reclamationIdField != null) {
            reclamationIdField.setEditable(false);
            reclamationIdField.setDisable(true);
        }
        if (messageArea != null) {
            messageArea.setEditable(false);
            messageArea.setDisable(true);
            messageArea.setPromptText("Read-only view for client.");
        }
        if (addButton != null) {
            addButton.setVisible(false);
            addButton.setManaged(false);
        }
        if (approveButton != null) {
            approveButton.setVisible(false);
            approveButton.setManaged(false);
        }
        if (updateButton != null) {
            updateButton.setVisible(false);
            updateButton.setManaged(false);
        }
        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        }
    }

    private boolean canAccessReclamation(int reclamationId) {
        try {
            Reclamation reclamation = reclamationService.findById(reclamationId);
            if (reclamation == null) {
                return false;
            }
            if (SessionContext.isAdmin()) {
                return true;
            }
            User current = SessionContext.getCurrentUser();
            return current != null && reclamation.getClientId() == current.getId();
        } catch (Exception e) {
            return false;
        }
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
        String message = safeText(reponse.getMessage(), "No response message.");
        String created = reponse.getCreatedAt() == null ? "Unknown date" : reponse.getCreatedAt().toString();
        return message + "\nCreated: " + created;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private VBox createResponseCard(Reponse reponse) {
        String raw = safeText(reponse.getMessage(), "No response message.");
        Label message = new Label(raw
                .replace(AI_DRAFT_PREFIX, "Draft for admin review")
                .replace(REVIEWED_PREFIX, "Reviewed response"));
        message.getStyleClass().add("response-item-message");
        message.setWrapText(true);

        String created = reponse.getCreatedAt() == null
                ? "Created: unknown"
                : "Created: " + reponse.getCreatedAt();
        Label meta = new Label(created);
        meta.getStyleClass().add("response-item-meta");

        VBox box = new VBox(6, message, meta);
        box.getStyleClass().add("response-item-card");
        return box;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
