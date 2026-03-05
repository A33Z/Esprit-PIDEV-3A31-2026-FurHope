package com.esprit.animal.controllers;

import com.esprit.animal.Services.AntiSpamAdoptionService;
import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.utils.Session;
import com.esprit.animal.utils.StageSceneHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterRequest extends BaseUIController {

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
    private Label clientNameLabel;
    @FXML
    private Label clientEmailLabel;
    @FXML
    private Label clientPhoneLabel;

    @FXML
    private TextArea message;

    @FXML
    private Button sendButton;

    private int animalId;
    private String animalName;
    private String animalSpecies;
    private String animalBreed;
    private String animalAgeGender;
    private Image animalImage;

    private final adoptionservices adoptionService = new adoptionservices();
    private final AntiSpamAdoptionService antiSpamService = new AntiSpamAdoptionService();

    @Override
    protected String getViewPath() {
        return "/animal/AjouterRequest.fxml";
    }

    @Override
    protected void onControllerReloaded(Object controller) {
        if (!(controller instanceof AjouterRequest reloaded)) {
            return;
        }

        if (animalId > 0) {
            reloaded.setAnimalInfo(animalId, animalName, animalSpecies, animalBreed, animalAgeGender, animalImage);
        }
        reloaded.setClientInfoFromSession();
        reloaded.message.setText(message.getText());
    }

    public void setAnimalInfo(int id, String name, String species, String breed, String ageGender, Image image) {
        this.animalId = id;
        this.animalName = name;
        this.animalSpecies = species;
        this.animalBreed = breed;
        this.animalAgeGender = ageGender;
        this.animalImage = image;

        animalNameLabel.setText(labelValue("name", name));
        animalSpeciesLabel.setText(labelValue("field.species", species));
        animalBreedLabel.setText(labelValue("field.breed", breed));
        animalAgeGenderLabel.setText(ageGender);

        if (image != null) {
            animalImageView.setImage(image);
        }
    }

    public void setClientInfoFromSession() {
        clientNameLabel.setText(labelValue("name", Session.getUserName()));
        clientEmailLabel.setText(labelValue("request.detail.email", Session.getUserEmail()));
        clientPhoneLabel.setText(labelValue("request.card.phone", String.valueOf(Session.getUserPhone())));
    }

    @FXML
    void Envoyer(ActionEvent event) {
        try {
            String msg = message.getText().trim();
            int clientCompteId = Session.getCompteId();

            if (msg.isEmpty()) {
                showAlert(tr("request.error.messageMissing.title"), tr("request.error.messageMissing.body"), Alert.AlertType.WARNING);
                return;
            }

            if (animalId <= 0 || clientCompteId <= 0) {
                showAlert(tr("request.error.invalid.title"), tr("request.error.invalid.body"), Alert.AlertType.ERROR);
                return;
            }

            AntiSpamAdoptionService.ValidationResult validation = antiSpamService.validateRequest(clientCompteId, animalId);
            if (!validation.isValid()) {
                showAlert(tr("request.error.blocked.title"), validation.getMessage(), Alert.AlertType.WARNING);
                return;
            }

            adoptionRequest request = new adoptionRequest(
                    animalId,
                    clientCompteId,
                    msg,
                    String.valueOf(Session.getUserPhone()),
                    "dummy address",
                    adoptionRequest.status.PENDING
            );
            adoptionService.ajouter(request);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle(tr("common.success"));
            success.setHeaderText(tr("common.success"));
            success.setContentText(tr("request.success.sent"));
            DialogPane dialogPane = success.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/animal/style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
            success.showAndWait();

            Parent root = loadView("/animal/AfficherAnimal.fxml");
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(false);
            stage.setMaximized(true);
            stage.show();

        } catch (SQLException e) {
            showAlert(tr("request.error.database"), e.getMessage(), Alert.AlertType.ERROR);
        } catch (IOException e) {
            showAlert(tr("request.error.file"), e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert(tr("request.error.unexpected"), e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String labelValue(String key, String value) {
        return tr(key) + ": " + (value == null ? "-" : value);
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}

