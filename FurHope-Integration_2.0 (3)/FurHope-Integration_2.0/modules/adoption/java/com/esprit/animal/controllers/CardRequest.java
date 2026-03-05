package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.Services.EmailNotificationService;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class CardRequest {
    @FXML
    private VBox cardRoot;
    @FXML
    private Label statusLabel;
    @FXML
    private ImageView animalImageView;
    @FXML
    private Label animalNameLabel;
    @FXML
    private Label applicantNameLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button approveButton;
    @FXML
    private Button declineButton;

    private adoptionRequest currentRequest;
    private final adoptionservices adoptionService = new adoptionservices();
    private final animalServices animalService = new animalServices();
    private final EmailNotificationService emailNotificationService = new EmailNotificationService();
    private Runnable refreshCallback;

    public void setRefreshCallback(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    public void setData(adoptionRequest request) {
        this.currentRequest = request;
        applyCardData(request);
        cardRoot.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Node && isInsideButton((Node) event.getTarget())) {
                return;
            }
            openDetail();
        });
    }

    @FXML
    private void approveFromCard(ActionEvent event) {
        processFromCard(event, adoptionRequest.status.APPROVED);
    }

    @FXML
    private void declineFromCard(ActionEvent event) {
        processFromCard(event, adoptionRequest.status.REJECTED);
    }

    private void applyCardData(adoptionRequest request) {
        animal pet = request.getAnimal();
        animalNameLabel.setText(labelValue("request.card.animal", pet != null ? pet.getName() : "-"));

        String applicantName = "-";
        if (request.getClientCompte() != null && request.getClientCompte().getUser() != null) {
            applicantName = request.getClientCompte().getUser().getName();
        }

        applicantNameLabel.setText(labelValue("request.card.applicant", applicantName));
        phoneLabel.setText(labelValue("request.card.phone", safeText(request.getPhone())));
        addressLabel.setText(labelValue("request.card.address", safeText(request.getAddress())));
        messageLabel.setText(safeText(request.getMessage(), tr("request.card.noMessage")));

        updateStatusDisplay(request.getStatus());
        loadAnimalImage(pet);
    }

    private void processFromCard(ActionEvent event, adoptionRequest.status newStatus) {
        if (currentRequest == null) {
            return;
        }

        if (currentRequest.getStatus() != adoptionRequest.status.PENDING) {
            showInfo(tr("request.card.alreadyProcessed"));
            return;
        }

        if (!confirmAction(newStatus)) {
            return;
        }

        try {
            currentRequest.setStatus(newStatus);
            adoptionService.modifier(currentRequest);

            if (newStatus == adoptionRequest.status.APPROVED && currentRequest.getAnimal() != null) {
                animal updatedAnimal = currentRequest.getAnimal();
                updatedAnimal.setStatus(animal.status.ADOPTED);
                animalService.modifier(updatedAnimal);
            }

            EmailNotificationService.NotificationResult emailResult =
                    emailNotificationService.sendDecisionNotification(currentRequest, newStatus);

            updateStatusDisplay(newStatus);

            if (refreshCallback != null) {
                refreshCallback.run();
            }

            String mainMessage = newStatus == adoptionRequest.status.APPROVED
                    ? tr("request.result.approved")
                    : tr("request.result.rejected");
            showInfo(composeResultMessage(mainMessage, emailResult));
            goToAnimalListing(event);
        } catch (Exception e) {
            showError(tr("request.error.process") + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openDetail() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/animal/requestDetailAdmin.fxml"), LanguageManager.getBundle());
            Parent root = loader.load();

            RequestDetailAdmin controller = loader.getController();
            controller.setRequest(currentRequest);

            Stage stage = (Stage) cardRoot.getScene().getWindow();
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToAnimalListing(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/animal/AfficherAnimal.fxml"), LanguageManager.getBundle());
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        StageSceneHelper.setScene(stage, root);
        stage.setMaximized(true);
        stage.show();
    }

    private boolean confirmAction(adoptionRequest.status newStatus) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(tr("request.confirm.title"));

        if (newStatus == adoptionRequest.status.APPROVED) {
            confirm.setHeaderText(tr("request.confirm.approveHeader"));
            confirm.setContentText(tr("request.confirm.approveContent"));
        } else {
            confirm.setHeaderText(tr("request.confirm.declineHeader"));
            confirm.setContentText(tr("request.confirm.declineContent"));
        }
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void updateStatusDisplay(adoptionRequest.status status) {
        statusLabel.setText(labelValue("request.card.status", statusToLabel(status)));
        switch (status) {
            case APPROVED:
                statusLabel.setStyle("-fx-background-color: #dcf4e4; -fx-text-fill: #1f7d48; -fx-font-weight: bold;");
                approveButton.setDisable(true);
                declineButton.setDisable(true);
                break;
            case REJECTED:
                statusLabel.setStyle("-fx-background-color: #fce4e4; -fx-text-fill: #b42318; -fx-font-weight: bold;");
                approveButton.setDisable(true);
                declineButton.setDisable(true);
                break;
            default:
                statusLabel.setStyle("-fx-background-color: #fff1dc; -fx-text-fill: #8a4600; -fx-font-weight: bold;");
                approveButton.setDisable(false);
                declineButton.setDisable(false);
                break;
        }
    }

    private String statusToLabel(adoptionRequest.status status) {
        return switch (status) {
            case PENDING -> tr("status.pending");
            case APPROVED -> tr("status.approved");
            case REJECTED -> tr("status.rejected");
        };
    }

    private void loadAnimalImage(animal pet) {
        if (pet == null || pet.getImage() == null || pet.getImage().isBlank()) {
            animalImageView.setImage(null);
            return;
        }
        try {
            File imageFile = new File("images/" + pet.getImage());
            if (imageFile.exists()) {
                animalImageView.setImage(new Image(imageFile.toURI().toString()));
            } else {
                animalImageView.setImage(null);
            }
        } catch (Exception e) {
            animalImageView.setImage(null);
        }
    }

    private boolean isInsideButton(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof Button) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tr("common.info"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tr("common.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }

    private String labelValue(String key, String value) {
        return tr(key) + ": " + safeText(value);
    }

    private String safeText(String value) {
        return safeText(value, "-");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String composeResultMessage(String mainMessage, EmailNotificationService.NotificationResult emailResult) {
        if (emailResult == null) {
            return mainMessage;
        }
        String prefix = emailResult.isSent() ? tr("email.notify.sent") : tr("email.notify.failed");
        return mainMessage + "\n\n" + prefix + " " + emailResult.getMessage();
    }
}



