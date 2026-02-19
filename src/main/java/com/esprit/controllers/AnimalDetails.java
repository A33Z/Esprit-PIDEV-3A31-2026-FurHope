package com.esprit.controllers;

import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
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

public class AnimalDetails {
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

    private animal animalSelected;
    private ListView<animal> listView;

    public void setAnimal(animal animal) {
        this.animalSelected = animal;

        nameLabel.setText(animal.getName());
        speciesLabel.setText(animal.getSpecies());
        breedLabel.setText(animal.getBreed());
        ageLabel.setText(String.valueOf(animal.getAge()));
        genderLabel.setText(animal.getGender().toString());
        descriptionLabel.setText(animal.getDescription());
        statusLabel.setText(animal.getStatus().toString());



        try {
            Image img = new Image(new File(animal.getImage()).toURI().toString());
            imageView.setImage(img);
        } catch (Exception e) {
            System.out.println("Image non trouvée");
        }
    }


    @FXML
    void handlemodifier(ActionEvent event) {

            try {
                // 1. Fermer la fenêtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();


                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalUpdate.fxml"));
                Parent root = loader.load();

                Animalupdate controller = loader.getController();
                controller.setAnimal(animalSelected);      // passer l'animal sélectionné
                controller.setListView(listView);        // passer la table pour refresh

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Animal");
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

            // fermer la fenêtre
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();


            // 3. Ouvrir la fenêtre d'affichage des animaux
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherAnimal.fxml"));
            Parent root = loader.load();

            // Récupérer le controller de la liste pour mettre à jour la TableView/ListView
            AfficherAnimal controller = loader.getController();
            controller.removeAnimalFromList(animalSelected); // méthode à créer dans le controller

            Stage listStage = new Stage();
            listStage.setScene(new Scene(root));
            listStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
