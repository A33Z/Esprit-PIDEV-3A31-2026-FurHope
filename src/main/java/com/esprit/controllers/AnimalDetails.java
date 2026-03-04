package com.esprit.controllers;

import com.esprit.Services.AnimalCareRecommendationService;
import com.esprit.Services.animalServices;
import com.esprit.entities.AnimalCareAdvice;
import com.esprit.entities.animal;
import com.esprit.i18n.LanguageManager;
import com.esprit.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class AnimalDetails extends BaseUIController {

    @FXML
    private Label ageLabel;
    @FXML
    private Label breedLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label genderLabel;
    @FXML
    private ImageView imageView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label speciesLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label ownerNameLabel;
    @FXML
    private Label ownerEmailLabel;
    @FXML
    private Label ownerPhoneLabel;
    @FXML
    private Label ownerRoleLabel;
    @FXML
    private Button modifyButton;
    @FXML
    private Button deleteButton;

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

    private animal animalSelected;
    private ListView<animal> listView;
    private final AnimalCareRecommendationService careRecommendationService = new AnimalCareRecommendationService();

    @Override
    protected String getViewPath() {
        return "/animalDetails.fxml";
    }

    @Override
    protected void onControllerReloaded(Object controller) {
        if (controller instanceof AnimalDetails reloaded && animalSelected != null) {
            reloaded.setListView(listView);
            reloaded.setAnimal(animalSelected);
        }
    }

    public void setListView(ListView<animal> listView) {
        this.listView = listView;
    }

    public void setAnimal(animal animal) {
        this.animalSelected = animal;

        nameLabel.setText(animal.getName());
        speciesLabel.setText(animal.getSpecies());
        breedLabel.setText(animal.getBreed());
        ageLabel.setText(String.valueOf(animal.getAge()));
        genderLabel.setText(localizeGender(animal.getGender()));
        descriptionLabel.setText(animal.getDescription());
        statusLabel.setText(localizeAnimalStatus(animal.getStatus()));

        if (animal.getImage() != null) {
            File file = new File("images/" + animal.getImage());
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        }

        if (animal.getOwnerCompte() != null && animal.getOwnerCompte().getUser() != null) {
            ownerNameLabel.setText(animal.getOwnerCompte().getUser().getName());
            ownerEmailLabel.setText(animal.getOwnerCompte().getUser().getEmail());
            ownerPhoneLabel.setText(String.valueOf(animal.getOwnerCompte().getUser().getPhone()));
            ownerRoleLabel.setText(animal.getOwnerCompte().getRole());
        }

        AnimalCareAdvice advice = careRecommendationService.generateCareAdvice(animal);
        careExerciseLabel.setText(advice.getExerciseRecommendation());
        careDietLabel.setText(advice.getDietRecommendation());
        careEnvironmentLabel.setText(advice.getEnvironmentRecommendation());
        careGroomingLabel.setText(advice.getGroomingRecommendation());
        careTrainingLabel.setText(advice.getTrainingRecommendation());

        boolean isOwner = animal.getOwnerCompteId() == Session.getCompteId();
        modifyButton.setVisible(isOwner);
        deleteButton.setVisible(isOwner);
    }

    @FXML
    void handlemodifier(ActionEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = createLoader("/AnimalUpdate.fxml");
            Parent root = loader.load();

            Animalupdate controller = loader.getController();
            controller.setAnimal(animalSelected);
            controller.setListView(listView);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(tr("page.addAnimal.title"));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handlesupprimer(ActionEvent event) {
        try {
            animalServices service = new animalServices();
            service.supprimer(animalSelected.getId());

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = createLoader("/AfficherAnimal.fxml");
            Parent root = loader.load();

            AfficherAnimal controller = loader.getController();
            controller.removeAnimalFromList(animalSelected);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

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
