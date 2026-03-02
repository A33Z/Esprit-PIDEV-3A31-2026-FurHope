package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.entities.animal;
import com.esprit.utils.MyDataBase;
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
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import static com.esprit.utils.Session.getUserId;

public class adopdetails {

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
    // Owner info labels
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

    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            centerContainer.setMinWidth(newVal.getWidth());
        });
        con = MyDataBase.getInstance().getConnection();

    }
    Connection con;

    private animal currentAnimal;  // store the selected animal


    public void setPetData(animal a) {
        this.currentAnimal = a;  // <-- store it


        petName.setText(a.getName());
        petStatus.setText(a.getStatus().toString());
        petGender.setText(a.getGender().toString());
        petAge.setText(a.getAge() + " Years");
        petSpecies.setText(a.getSpecies());
        petBreed.setText(a.getBreed());
        petDescription.setText(a.getDescription());



        if (a.getImage() != null) {
            File file = new File("images/" + a.getImage());
            if (file.exists()) {
                petImage.setImage(new Image(file.toURI().toString()));
            }
        }
        if (a.getOwner() != null) {
            ownerNameLabel.setText(a.getOwner().getName());
            ownerEmailLabel.setText(a.getOwner().getEmail());
            ownerPhoneLabel.setText(String.valueOf(a.getOwner().getPhone()));
            ownerRoleLabel.setText(a.getOwner().getRole());
        }


    }


    @FXML
    private void handleAdopt() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRequest.fxml"));
            Parent root = loader.load();

            com.esprit.controllers.AjouterRequest controller = loader.getController();

            // Préparer l'image correctement
            Image animalImage = null;
            if (currentAnimal.getImage() != null) {
                File file = new File("images/" + currentAnimal.getImage());
                if (file.exists()) {
                    animalImage = new Image(file.toURI().toString());
                } else {
                    System.out.println("Image introuvable : " + file.getAbsolutePath());
                }
            }

            // Passer toutes les infos à AjouterRequest
            controller.setAnimalInfo(
                    currentAnimal.getId(),
                    currentAnimal.getName(),
                    currentAnimal.getSpecies(),
                    currentAnimal.getBreed(),
                    currentAnimal.getAge() + " - " + currentAnimal.getGender(),
                    animalImage
            );

            Stage stage = new Stage();
            stage.setTitle("Add Adoption Request");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) adoptButton.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
