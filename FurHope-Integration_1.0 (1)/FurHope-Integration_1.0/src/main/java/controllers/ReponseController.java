package com.esprit.controllers;

import controllers.SessionContext;
import com.esprit.entities.Reclamation;
import com.esprit.entities.Reponse;
import com.esprit.services.ReclamationService;
import com.esprit.services.ReponseService;
import com.esprit.services.ai.AIResponseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReponseController {

    @FXML
    private ListView<ChatEntry> reponseTable;

    @FXML
    private TextField reclamationIdField;
    @FXML
    private TextArea messageArea;

    @FXML
    private Label ticketIdLabel;
    @FXML
    private Label ticketStatusLabel;
    @FXML
    private Label ticketClientLabel;
    @FXML
    private Button generateAiButton;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    private final ReponseService service = new ReponseService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final AIResponseService aiResponseService = new AIResponseService();
    private Reponse selectedResponse;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Please login first.");
            return;
        }

        boolean admin = SessionContext.isAdmin();
        if (!admin) {
            setAdminControlsVisible(false);
            messageArea.setEditable(false);
            messageArea.setPromptText("Read-only thread");
        }

        reponseTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ChatEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(buildMessageRow(item));
            }
        });

        Integer selectedReclamation = SessionContext.getSelectedReclamationId();
        if (selectedReclamation != null) {
            reclamationIdField.setText(String.valueOf(selectedReclamation));
            loadByReclamation(selectedReclamation);
        } else {
            refreshTable();
        }

        reponseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> fillForm(selected == null ? null : selected.reponse));
    }

    @FXML
    private void addReponse() {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can add responses.");
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
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can update responses.");
            return;
        }
        Reponse selected = selectedResponse;
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
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can delete responses.");
            return;
        }
        Reponse selected = selectedResponse;
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
                renderConversation(null, data);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load responses.");
        }
    }

    @FXML
    private void generateAiResponse() {
        String reclamationIdRaw = reclamationIdField.getText() == null ? "" : reclamationIdField.getText().trim();
        if (reclamationIdRaw.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter or select a reclamation ID first.");
            return;
        }

        int reclamationId;
        try {
            reclamationId = Integer.parseInt(reclamationIdRaw);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
            return;
        }

        Reclamation reclamation;
        try {
            reclamation = reclamationService.findById(reclamationId);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load reclamation details.");
            return;
        }

        if (reclamation == null) {
            showAlert(Alert.AlertType.WARNING, "Not Found", "No reclamation found with this ID.");
            return;
        }

        String complaintText = buildComplaintText(reclamation);
        if (complaintText.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Selected reclamation has no content to analyze.");
            return;
        }

        messageArea.setPromptText("Generating AI response...");

        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return aiResponseService.generateSmartResponse(complaintText);
            }
        };

        aiTask.setOnSucceeded(e -> {
            messageArea.setPromptText("Write admin response");
            messageArea.setText(aiTask.getValue());
        });

        aiTask.setOnFailed(e -> {
            messageArea.setPromptText("Write admin response");
            Throwable ex = aiTask.getException();
            String msg = ex == null ? "AI generation failed." : ex.getMessage();
            showAlert(Alert.AlertType.ERROR, "AI Error", msg);
        });

        Thread worker = new Thread(aiTask, "ai-response-generator");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/reclamation.fxml");
    }

    private void loadByReclamation(int reclamationId) {
        try {
            Reclamation reclamation = reclamationService.findById(reclamationId);
            ObservableList<Reponse> data = FXCollections.observableArrayList(service.afficherParReclamation(reclamationId));
            renderConversation(reclamation, data);
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
        selectedResponse = null;
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void renderConversation(Reclamation reclamation, ObservableList<Reponse> responses) {
        ObservableList<ChatEntry> conversation = FXCollections.observableArrayList();
        if (reclamation != null) {
            ticketIdLabel.setText("Reclamation #: " + reclamation.getId());
            ticketStatusLabel.setText("Status: " + normalizeStatus(reclamation.getStatus()));
            ticketClientLabel.setText("Client: #" + reclamation.getClientId());
            conversation.add(new ChatEntry(
                    false,
                    "Client",
                    safeText(reclamation.getDescription()),
                    reclamation.getCreatedAt(),
                    null
            ));
        } else {
            ticketIdLabel.setText("Reclamation #: -");
            ticketStatusLabel.setText("Status: -");
            ticketClientLabel.setText("Client: -");
        }

        for (Reponse response : responses) {
            conversation.add(new ChatEntry(
                    true,
                    "Admin #" + response.getAdminId(),
                    safeText(response.getMessage()),
                    response.getCreatedAt(),
                    response
            ));
        }
        reponseTable.setItems(conversation);
    }

    private HBox buildMessageRow(ChatEntry item) {
        Label authorLabel = new Label(item.author);
        authorLabel.getStyleClass().add("bubble-author");

        Label textLabel = new Label(item.message);
        textLabel.getStyleClass().add("bubble-text");
        textLabel.setWrapText(true);

        Label timeLabel = new Label(formatDate(item.createdAt));
        timeLabel.getStyleClass().add("bubble-time");

        VBox bubble = new VBox(4, authorLabel, textLabel, timeLabel);
        bubble.getStyleClass().add(item.adminMessage ? "bubble-right" : "bubble-left");
        bubble.setMaxWidth(520);
        bubble.setPadding(new Insets(10, 12, 10, 12));

        HBox row = new HBox();
        row.getStyleClass().add("message-row");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (item.adminMessage) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(bubble, spacer);
        }

        if (item.reponse != null) {
            row.setOnMouseClicked(e -> {
                selectedResponse = item.reponse;
                fillForm(item.reponse);
                reponseTable.getSelectionModel().select(item);
            });
        }
        return row;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return DATE_FORMAT.format(dateTime);
    }

    private void setAdminControlsVisible(boolean visible) {
        if (generateAiButton != null) {
            generateAiButton.setVisible(visible);
            generateAiButton.setManaged(visible);
        }
        if (addButton != null) {
            addButton.setVisible(visible);
            addButton.setManaged(visible);
        }
        if (updateButton != null) {
            updateButton.setVisible(visible);
            updateButton.setManaged(visible);
        }
        if (deleteButton != null) {
            deleteButton.setVisible(visible);
            deleteButton.setManaged(visible);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String buildComplaintText(Reclamation reclamation) {
        String subject = safeText(reclamation.getSujet());
        String description = safeText(reclamation.getDescription());
        if (subject.isBlank() && description.isBlank()) {
            return "";
        }
        if (subject.isBlank()) {
            return description;
        }
        if (description.isBlank()) {
            return "Subject: " + subject;
        }
        return "Subject: " + subject + "\nComplaint: " + description;
    }

    private static final class ChatEntry {
        private final boolean adminMessage;
        private final String author;
        private final String message;
        private final LocalDateTime createdAt;
        private final Reponse reponse;

        private ChatEntry(boolean adminMessage, String author, String message, LocalDateTime createdAt, Reponse reponse) {
            this.adminMessage = adminMessage;
            this.author = author;
            this.message = message;
            this.createdAt = createdAt;
            this.reponse = reponse;
        }
    }
}
