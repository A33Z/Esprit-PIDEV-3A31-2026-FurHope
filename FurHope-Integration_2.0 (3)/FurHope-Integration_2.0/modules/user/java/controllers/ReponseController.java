package com.esprit.controllers;

import controllers.SessionContext;
import com.esprit.entities.Reclamation;
import com.esprit.entities.Reponse;
import com.esprit.services.ReclamationAiAssistantService;
import com.esprit.services.ReclamationService;
import com.esprit.services.ReponseService;
import entities.User;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReponseController {

    @FXML
    private VBox reponseCardsPane;
    @FXML
    private TextField reclamationIdField;
    @FXML
    private TextArea messageArea;

    private final ReponseService reponseService = new ReponseService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final ReclamationAiAssistantService aiAssistant = new ReclamationAiAssistantService();
    private final ObservableList<Reponse> sourceData = FXCollections.observableArrayList();
    private Reponse selectedReponse;

    @FXML
    private void initialize() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        Integer selectedReclamation = SessionContext.getSelectedReclamationId();
        if (selectedReclamation != null) {
            reclamationIdField.setText(String.valueOf(selectedReclamation));
            refreshTable();
        } else if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Open chat from a reclamation card.");
        } else {
            refreshTable();
        }
    }

    @FXML
    private void addReponse() {
        if (!validate()) {
            return;
        }
        User current = SessionContext.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        int reclamationId;
        try {
            reclamationId = Integer.parseInt(reclamationIdField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
            return;
        }

        try {
            if (!canAccessReclamation(reclamationId)) {
                showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot access this conversation.");
                return;
            }

            String message = messageArea.getText().trim();
            String badWord = aiAssistant.findFirstProfanity(message);
            if (badWord != null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Inappropriate language detected: " + badWord);
                return;
            }

            Reponse reponse = new Reponse();
            reponse.setReclamationId(reclamationId);
            reponse.setAdminId(SessionContext.isAdmin() ? current.getId() : 0);
            reponse.setSenderId(current.getId());
            reponse.setSenderType(SessionContext.isAdmin() ? "ADMIN" : "CLIENT");
            reponse.setMessage(message);
            reponseService.ajouter(reponse);

            if (!SessionContext.isAdmin()) {
                addAiFollowUp(reclamationId, message);
            }

            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add message.");
        }
    }

    @FXML
    private void updateReponse() {
        if (selectedReponse == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a message first.");
            return;
        }
        if (!canModifySelected(selectedReponse)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot edit this message.");
            return;
        }
        if (!validate()) {
            return;
        }

        try {
            String message = messageArea.getText().trim();
            String badWord = aiAssistant.findFirstProfanity(message);
            if (badWord != null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Inappropriate language detected: " + badWord);
                return;
            }

            selectedReponse.setMessage(message);
            reponseService.modifier(selectedReponse);
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update message.");
        }
    }

    @FXML
    private void deleteReponse() {
        if (selectedReponse == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a message first.");
            return;
        }
        if (!canModifySelected(selectedReponse)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot delete this message.");
            return;
        }

        try {
            reponseService.supprimer(selectedReponse.getId());
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete message.");
        }
    }

    @FXML
    private void refreshTable() {
        try {
            String value = reclamationIdField.getText() == null ? "" : reclamationIdField.getText().trim();
            if (value.isEmpty()) {
                if (!SessionContext.isAdmin()) {
                    showAlert(Alert.AlertType.WARNING, "Selection Required", "Enter a reclamation ID.");
                    return;
                }
                sourceData.setAll(reponseService.afficher());
            } else {
                int reclamationId = Integer.parseInt(value);
                if (!canAccessReclamation(reclamationId)) {
                    showAlert(Alert.AlertType.WARNING, "Access Denied", "You cannot access this conversation.");
                    return;
                }
                sourceData.setAll(reponseService.afficherParReclamation(reclamationId));
            }
            renderChat(sourceData);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID must be numeric.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load messages.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/reclamation.fxml");
    }

    private void renderChat(List<Reponse> list) {
        reponseCardsPane.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("No messages yet. Start the conversation.");
            empty.getStyleClass().add("card-subtitle");
            reponseCardsPane.getChildren().add(empty);
            return;
        }
        for (Reponse reponse : list) {
            reponseCardsPane.getChildren().add(buildMessageRow(reponse));
        }
    }

    private HBox buildMessageRow(Reponse reponse) {
        boolean mine = isMine(reponse);
        VBox bubble = new VBox(6);
        bubble.getStyleClass().addAll("card", "chat-bubble", mine ? "chat-bubble-mine" : "chat-bubble-other");
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(640);

        Label sender = new Label(senderLabel(reponse));
        sender.getStyleClass().add("field-label");
        Label message = new Label(reponse.getMessage() == null ? "" : reponse.getMessage());
        message.setWrapText(true);
        message.getStyleClass().add("card-subtitle");
        Label meta = new Label(formatDate(reponse.getCreatedAt()) + ratingSuffix(reponse));
        meta.getStyleClass().add("field-label");

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(meta);

        if (SessionContext.isAdmin()) {
            HBox ratings = new HBox(4);
            ratings.setAlignment(Pos.CENTER_LEFT);
            for (int i = 1; i <= 5; i++) {
                final int value = i;
                Button star = new Button(String.valueOf(i));
                star.getStyleClass().addAll("secondary-button", "action-button-small");
                star.setOnAction(e -> rateMessage(reponse, value));
                ratings.getChildren().add(star);
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            footer.getChildren().addAll(spacer, new Label("Rate:"), ratings);
        }

        bubble.getChildren().addAll(sender, message, footer);
        bubble.setOnMouseClicked(e -> selectMessage(reponse));

        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        if (mine) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }
        row.getChildren().add(bubble);
        return row;
    }

    private void rateMessage(Reponse reponse, int rating) {
        if (!SessionContext.isAdmin()) {
            return;
        }
        try {
            reponseService.rateReponse(reponse.getId(), rating);
            refreshTable();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to save rating.");
        }
    }

    private void selectMessage(Reponse reponse) {
        selectedReponse = reponse;
        fillForm(reponse);
    }

    private boolean validate() {
        if (reclamationIdField.getText().trim().isEmpty() || messageArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Reclamation ID and message are required.");
            return false;
        }
        return true;
    }

    private boolean canModifySelected(Reponse reponse) {
        if (reponse == null) {
            return false;
        }
        if (reponse.isAiMessage()) {
            return false;
        }
        User current = SessionContext.getCurrentUser();
        if (current == null) {
            return false;
        }
        if (SessionContext.isAdmin()) {
            return reponse.isAdminMessage();
        }
        return reponse.isClientMessage() && reponse.getSenderId() == current.getId();
    }

    private boolean canAccessReclamation(int reclamationId) {
        try {
            if (SessionContext.isAdmin()) {
                return true;
            }
            User current = SessionContext.getCurrentUser();
            return current != null && reclamationService.isOwnedByClient(reclamationId, current.getId());
        } catch (Exception e) {
            return false;
        }
    }

    private void addAiFollowUp(int reclamationId, String clientMessage) {
        try {
            Reponse aiReply = new Reponse();
            aiReply.setReclamationId(reclamationId);
            aiReply.setAdminId(-1);
            aiReply.setSenderId(-1);
            aiReply.setSenderType("AI");
            aiReply.setMessage(aiAssistant.buildChatReply(clientMessage));
            reponseService.ajouter(aiReply);
        } catch (Exception e) {
            System.err.println("AI follow-up warning: " + e.getMessage());
        }
    }

    private void fillForm(Reponse reponse) {
        if (reponse == null) {
            return;
        }
        reclamationIdField.setText(String.valueOf(reponse.getReclamationId()));
        messageArea.setText(reponse.getMessage());
    }

    private void clearForm() {
        selectedReponse = null;
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

    private String senderLabel(Reponse reponse) {
        if (reponse == null) {
            return "Unknown";
        }
        String type = reponse.getSenderType() == null ? "" : reponse.getSenderType().toUpperCase(Locale.ROOT);
        if ("AI".equals(type)) {
            return "AI Assistant";
        }
        if ("CLIENT".equals(type)) {
            return "Client #" + reponse.getSenderId();
        }
        return "Admin #" + reponse.getSenderId();
    }

    private boolean isMine(Reponse reponse) {
        User current = SessionContext.getCurrentUser();
        if (current == null || reponse == null) {
            return false;
        }
        return reponse.getSenderId() == current.getId();
    }

    private String ratingSuffix(Reponse reponse) {
        return reponse.getRating() == null ? "" : "  •  Rating: " + reponse.getRating() + "/5";
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "No date";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
