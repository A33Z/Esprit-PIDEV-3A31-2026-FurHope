package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.AnimalCareRecommendationService;
import com.esprit.animal.entities.AnimalCareAdvice;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;

public class adopdetails extends BaseUIController {

    @FXML
    private ImageView petImage;
    @FXML
    private Label petName;
    @FXML
    private Label petStatus;
    @FXML
    private Label petGender;
    @FXML
    private Label petAge;
    @FXML
    private Label petSpecies;
    @FXML
    private Label petBreed;
    @FXML
    private Label petDescription;
    @FXML
    private Button adoptButton;
    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Label ownerNameLabel;
    @FXML
    private Label ownerEmailLabel;
    @FXML
    private Label ownerPhoneLabel;
    @FXML
    private Label ownerRoleLabel;
    @FXML
    private StackPane centerContainer;

    @FXML
    private Label careExerciseLabel;
    @FXML
    private Label careDietLabel;
    @FXML
    private Label careEnvironmentLabel;
    @FXML
    private Label careGroomingLabel;
    @FXML
    private Label careTrainingLabel;

    private Connection con;
    private animal currentAnimal;
    private final AnimalCareRecommendationService careRecommendationService = new AnimalCareRecommendationService();

    @Override
    protected String getViewPath() {
        return "/animal/adopanimaldetails.fxml";
    }

    @Override
    protected void onControllerReloaded(Object controller) {
        if (controller instanceof adopdetails reloaded && currentAnimal != null) {
            reloaded.setPetData(currentAnimal);
        }
    }

    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> centerContainer.setMinWidth(newVal.getWidth()));
        con = MyDataBase.getInstance().getConnection();
    }

    public void setPetData(animal a) {
        this.currentAnimal = a;

        petName.setText(a.getName());
        petStatus.setText(localizeAnimalStatus(a.getStatus()));
        petGender.setText(localizeGender(a.getGender()));
        petAge.setText(a.getAge() + " " + tr("animal.card.years"));
        petSpecies.setText(a.getSpecies());
        petBreed.setText(a.getBreed());
        petDescription.setText(a.getDescription());

        if (a.getImage() != null) {
            File file = new File("images/" + a.getImage());
            if (file.exists()) {
                petImage.setImage(new Image(file.toURI().toString()));
            }
        }

        if (a.getOwnerCompte() != null && a.getOwnerCompte().getUser() != null) {
            ownerNameLabel.setText(a.getOwnerCompte().getUser().getName());
            ownerEmailLabel.setText(a.getOwnerCompte().getUser().getEmail());
            ownerPhoneLabel.setText(String.valueOf(a.getOwnerCompte().getUser().getPhone()));
            ownerRoleLabel.setText(a.getOwnerCompte().getRole());
        }

        AnimalCareAdvice advice = careRecommendationService.generateCareAdvice(a);
        careExerciseLabel.setText(advice.getExerciseRecommendation());
        careDietLabel.setText(advice.getDietRecommendation());
        careEnvironmentLabel.setText(advice.getEnvironmentRecommendation());
        careGroomingLabel.setText(advice.getGroomingRecommendation());
        careTrainingLabel.setText(advice.getTrainingRecommendation());
    }

    @FXML
    private void handleAdopt() {
        try {
            FXMLLoader loader = createLoader("/animal/AjouterRequest.fxml");
            Parent root = loader.load();

            AjouterRequest controller = loader.getController();

            Image animalImage = null;
            if (currentAnimal.getImage() != null) {
                File file = new File("images/" + currentAnimal.getImage());
                if (file.exists()) {
                    animalImage = new Image(file.toURI().toString());
                } else {
                    System.out.println("Image not found: " + file.getAbsolutePath());
                }
            }

            controller.setAnimalInfo(
                    currentAnimal.getId(),
                    currentAnimal.getName(),
                    currentAnimal.getSpecies(),
                    currentAnimal.getBreed(),
                    currentAnimal.getAge() + " - " + currentAnimal.getGender(),
                    animalImage
            );
            controller.setClientInfoFromSession();

            Stage stage = new Stage();
            stage.setTitle(tr("page.addRequest.title"));
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) adoptButton.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String localizeAnimalStatus(animal.status status) {
        return switch (status) {
            case AVAILABLE -> tr("status.available");
            case UNAVAILABLE -> tr("status.unavailable");
            case ADOPTED -> tr("status.adopted");
        };
    }

    private String localizeGender(animal.gender gender) {
        return switch (gender) {
            case MALE -> tr("gender.male");
            case FEMALE -> tr("gender.female");
        };
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}



