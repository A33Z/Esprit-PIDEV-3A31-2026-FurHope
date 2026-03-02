package com.esprit.controllers;

import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
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

    @FXML
    private Label ownerNameLabel;
    @FXML
    private Label ownerEmailLabel;
    @FXML
    private Label ownerPhoneLabel;
    @FXML
    private Label ownerRoleLabel;

    @FXML private Button modifyButton;
    @FXML private Button deleteButton;


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



        if (animal.getImage() != null) {
            File file = new File("images/" + animal.getImage());
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        }

        // Affichage infos owner
        if (animal.getOwner() != null) {
            ownerNameLabel.setText(animal.getOwner().getName());
            ownerEmailLabel.setText(animal.getOwner().getEmail());
            ownerPhoneLabel.setText(String.valueOf(animal.getOwner().getPhone()));
            ownerRoleLabel.setText(animal.getOwner().getRole());
        }

        // 🔹 Gestion dynamique des boutons
        boolean isOwner = animal.getOwnerid() == Session.getUserId();

        modifyButton.setVisible(isOwner);
        deleteButton.setVisible(isOwner);

    }


    @FXML
    void handlemodifier(ActionEvent event) {

            try {
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();


                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalUpdate.fxml"));
                Parent root = loader.load();

                Animalupdate controller = loader.getController();
                controller.setAnimal(animalSelected);
                controller.setListView(listView);

                // Create a new stage for the details page
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Animal");


                // Maximize the new window
                stage.setMaximized(true);

                // Show the details window
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


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherAnimal.fxml"));
            Parent root = loader.load();

            AfficherAnimal controller = loader.getController();
            controller.removeAnimalFromList(animalSelected);

            // Create a new stage for the details page
            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            // Maximize the new window
            stage.setMaximized(true);

            // Show the details window
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
