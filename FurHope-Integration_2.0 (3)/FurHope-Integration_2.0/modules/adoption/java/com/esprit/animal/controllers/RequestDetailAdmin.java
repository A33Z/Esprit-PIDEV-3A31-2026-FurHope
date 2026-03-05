package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.Services.EmailNotificationService;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;

public class RequestDetailAdmin extends BaseUIController {

    @FXML
    private Label animalNameLabel;
    @FXML
    private Label animalSpeciesLabel;
    @FXML
    private Label animalBreedLabel;
    @FXML
    private Label animalAgeGenderLabel;
    @FXML
    private ImageView animalImageView;

    @FXML
    private Label requesterNameLabel;
    @FXML
    private Label requesterEmailLabel;
    @FXML
    private Label requesterPhoneLabel;
    @FXML
    private Label requesterAddressLabel;

    @FXML
    private Label statusLabel;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button approveButton;
    @FXML
    private Button declineButton;
    @FXML
    private Button closeButton;

    private adoptionRequest currentRequest;
    private final adoptionservices adoptionService = new adoptionservices();
    private final animalServices animalService = new animalServices();
    private final EmailNotificationService emailNotificationService = new EmailNotificationService();

    @Override
    protected String getViewPath() {
        return "/animal/requestDetailAdmin.fxml";
    }

    @Override
    protected String getBackViewPath() {
        return "/animal/Requests.fxml";
    }

    @FXML
    public void initialize() {
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
    }

    public void setRequest(adoptionRequest request) {
        this.currentRequest = request;

        animal a = request.getAnimal();

        if (a != null) {
            animalNameLabel.setText(labelValue("request.card.animal", a.getName()));
            animalSpeciesLabel.setText(labelValue("request.detail.species", a.getSpecies()));
            animalBreedLabel.setText(labelValue("request.detail.breed", a.getBreed()));
            animalAgeGenderLabel.setText(
                    tr("request.detail.age") + ": " + a.getAge() + " | " +
                            tr("request.detail.gender") + ": " +
                            (a.getGender() == animal.gender.MALE ? tr("gender.male") : tr("gender.female"))
            );

            if (a.getImage() != null && !a.getImage().isEmpty()) {
                try {
                    File imageFile = new File("images/" + a.getImage());
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        animalImageView.setImage(image);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur chargement image: " + e.getMessage());
                }
            }
        }

        if (request.getClientCompte() != null && request.getClientCompte().getUser() != null) {
            requesterNameLabel.setText(labelValue("request.card.applicant", request.getClientCompte().getUser().getName()));
            requesterEmailLabel.setText(labelValue("request.detail.email", request.getClientCompte().getUser().getEmail()));
            requesterPhoneLabel.setText(labelValue("request.card.phone", String.valueOf(request.getClientCompte().getUser().getPhone())));
        } else {
            requesterNameLabel.setText(labelValue("request.detail.clientId", String.valueOf(request.getClientCompteId())));
            requesterEmailLabel.setText(labelValue("request.detail.email", "-"));
            requesterPhoneLabel.setText(labelValue("request.card.phone", "-"));
        }

        requesterAddressLabel.setText(labelValue("request.card.address", safeText(request.getAddress(), "-")));
        messageArea.setText(safeText(request.getMessage(), tr("request.card.noMessage")));

        updateStatusLabel();

        approveButton.setOnAction(e -> approveRequest());
        declineButton.setOnAction(e -> declineRequest());
        closeButton.setOnAction(e -> closeWindow());
    }

    @FXML
    private void approveRequest() {
        if (currentRequest == null) {
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(tr("request.confirm.title"));
        confirmAlert.setHeaderText(tr("request.confirm.approveHeader"));
        confirmAlert.setContentText(tr("request.confirm.approveContent"));

        if (confirmAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.OK) {
            return;
        }

        try {
            currentRequest.setStatus(adoptionRequest.status.APPROVED);
            adoptionService.modifier(currentRequest);

            if (currentRequest.getAnimal() != null) {
                animal animalToUpdate = currentRequest.getAnimal();
                animalToUpdate.setStatus(animal.status.ADOPTED);
                animalService.modifier(animalToUpdate);
            }

            EmailNotificationService.NotificationResult emailResult =
                    emailNotificationService.sendDecisionNotification(currentRequest, adoptionRequest.status.APPROVED);

            showSuccess(tr("common.success"), composeResultMessage(tr("request.result.approved"), emailResult));
            updateStatusLabel();
            disableButtons();
            goToAnimalListing();

        } catch (Exception e) {
            showError(tr("common.error"), tr("request.error.approve") + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void declineRequest() {
        if (currentRequest == null) {
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(tr("request.confirm.title"));
        confirmAlert.setHeaderText(tr("request.confirm.declineHeader"));
        confirmAlert.setContentText(tr("request.confirm.declineContent"));

        if (confirmAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.OK) {
            return;
        }

        try {
            currentRequest.setStatus(adoptionRequest.status.REJECTED);
            adoptionService.modifier(currentRequest);

            EmailNotificationService.NotificationResult emailResult =
                    emailNotificationService.sendDecisionNotification(currentRequest, adoptionRequest.status.REJECTED);

            showSuccess(tr("common.success"), composeResultMessage(tr("request.result.rejected"), emailResult));
            updateStatusLabel();
            disableButtons();
            goToAnimalListing();

        } catch (Exception e) {
            showError(tr("common.error"), tr("request.error.decline") + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatusLabel() {
        if (currentRequest == null) {
            return;
        }

        String status = switch (currentRequest.getStatus()) {
            case APPROVED -> tr("status.approved");
            case REJECTED -> tr("status.rejected");
            case PENDING -> tr("status.pending");
        };
        statusLabel.setText(tr("request.card.status") + ": " + status);

        switch (currentRequest.getStatus()) {
            case APPROVED:
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                break;
            case REJECTED:
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                break;
            case PENDING:
                statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                break;
        }
    }

    private void disableButtons() {
        approveButton.setDisable(true);
        declineButton.setDisable(true);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void goToAnimalListing() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/animal/AfficherAnimal.fxml"), LanguageManager.getBundle());
        Parent root = loader.load();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        StageSceneHelper.setScene(stage, root);
        stage.setMaximized(true);
        stage.show();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
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
        return tr(key) + ": " + safeText(value, "-");
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



