package controllers;

import application.AppContext;
import application.controller.SupportChatController;
import application.model.SupportChatRequestModel;
import application.model.SupportChatResponseModel;
import entities.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.AuthorizationException;
import services.SessionContext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SupportCenterController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label sessionLabel;
    @FXML
    private Label supportStatusLabel;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatMessagesContainer;
    @FXML
    private TextField chatInputField;
    @FXML
    private Button sendButton;

    private SupportChatController supportChatController;

    @FXML
    public void initialize() {
        User user;
        try {
            user = SessionContext.requireNormalUser();
        } catch (AuthorizationException e) {
            Platform.runLater(this::redirectToRoleSelection);
            return;
        }

        sessionLabel.setText("Logged in as: " + user.getDisplayName());
        supportStatusLabel.setText("");

        try {
            supportChatController = AppContext.getInstance().supportChatController();
        } catch (RuntimeException e) {
            supportChatController = null;
            showStatus("AI support is currently unavailable.", true);
        }

        appendMessage(
                "Hello. I can help with reservation status, booking changes, cancellations, hotel information, and FAQs.",
                false
        );
    }

    @FXML
    private void handleSendMessage() {
        if (supportChatController == null) {
            showStatus("Support service is unavailable right now.", true);
            return;
        }

        String userMessage = normalize(chatInputField.getText());
        if (userMessage.isBlank()) {
            return;
        }

        appendMessage(userMessage, true);
        chatInputField.clear();
        showStatus("", false);
        setComposerDisabled(true);

        Task<SupportChatResponseModel> task = new Task<>() {
            @Override
            protected SupportChatResponseModel call() {
                return supportChatController.postSupportChat(new SupportChatRequestModel(userMessage));
            }
        };
        task.setOnSucceeded(event -> {
            SupportChatResponseModel response = task.getValue();
            String assistantMessage = response == null ? "" : response.response();
            if (assistantMessage == null || assistantMessage.isBlank()) {
                assistantMessage = "I could not generate a response. Please try again.";
            }
            appendMessage(assistantMessage, false);
            setComposerDisabled(false);
            chatInputField.requestFocus();
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            if (error instanceof AuthorizationException) {
                showStatus("Session expired. Please sign in again.", true);
                setComposerDisabled(false);
                redirectToRoleSelection();
                return;
            }
            appendMessage("Support is temporarily unavailable. Please try again shortly.", false);
            showStatus("Unable to process this request right now.", true);
            setComposerDisabled(false);
            chatInputField.requestFocus();
        });
        runTask(task, "support-chat-thread");
    }

    @FXML
    private void handleCloseSupportCenter() {
        if (rootPane.getScene() == null) {
            return;
        }
        Stage stage = (Stage) rootPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void appendMessage(String content, boolean fromUser) {
        String text = normalize(content);
        if (text.isBlank()) {
            return;
        }

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(640);
        bubble.getStyleClass().addAll(
                "support-message-bubble",
                fromUser ? "support-user-bubble" : "support-assistant-bubble"
        );

        Label timestamp = new Label(TIME_FORMATTER.format(LocalDateTime.now()));
        timestamp.getStyleClass().add("support-message-time");

        VBox bubbleBlock = new VBox(3, bubble, timestamp);
        bubbleBlock.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(bubbleBlock);
        row.getStyleClass().add("support-message-row");
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        chatMessagesContainer.getChildren().add(row);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void setComposerDisabled(boolean disabled) {
        chatInputField.setDisable(disabled);
        sendButton.setDisable(disabled);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String compact = value
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replace('\u0000', ' ')
                .trim();
        if (compact.isBlank()) {
            return "";
        }

        String[] lines = compact.split("\\R");
        StringBuilder normalized = new StringBuilder();
        for (String line : lines) {
            String sanitizedLine = line.replaceAll("\\s+", " ").trim();
            if (sanitizedLine.isBlank()) {
                continue;
            }
            if (normalized.length() > 0) {
                normalized.append('\n');
            }
            normalized.append(sanitizedLine);
        }
        return normalized.toString().trim();
    }

    private void showStatus(String message, boolean error) {
        supportStatusLabel.setText(message == null ? "" : message.trim());
        supportStatusLabel.getStyleClass().removeAll("form-error", "header-subtitle");
        supportStatusLabel.getStyleClass().add(error ? "form-error" : "header-subtitle");
    }

    private void runTask(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void redirectToRoleSelection() {
        if (rootPane.getScene() == null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    redirectToRoleSelection();
                }
            });
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RoleSelection.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FurHope - Access Portal");
            stage.show();
        } catch (IOException e) {
            showStatus("Unable to return to the access portal.", true);
        }
    }
}

