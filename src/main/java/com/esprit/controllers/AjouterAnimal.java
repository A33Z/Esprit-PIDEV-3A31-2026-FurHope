package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import com.esprit.Services.animalServices;
import com.esprit.entities.animal;

import javafx.scene.image.ImageView;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class AjouterAnimal {

    @FXML
    private TextField age;

    @FXML
    private TextField breed;

    @FXML
    private TextField description;

    @FXML
    private ComboBox<String> gender;

    @FXML
    private TextField name;

    @FXML
    private TextField species;

    @FXML
    private ComboBox<String> status;

    @FXML
    private ImageView imagePreview;

    private String image;
   // private String imagePath;
    animalServices service = new animalServices();


    @FXML
    public void initialize() {

        gender.getItems().addAll("MALE", "FEMALE");

        status.getItems().addAll("AVAILABLE", "ADOPTED", "UNAVAILABLE");

    }

    @FXML
    void save(ActionEvent event) {
        try {

            // 1️⃣ Ajouter animal dans la base
            service.ajouter(new animal(
                    name.getText(),
                    species.getText(),
                    breed.getText(),
                    Integer.parseInt(age.getText()),
                    animal.gender.valueOf(gender.getValue()),
                    description.getText(),
                    animal.status.valueOf(status.getValue()),
                    image
            ));

            // 2️⃣ Charger page affichage
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            name.getScene().setRoot(root);

        } catch (SQLException e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }}

    @FXML
    void afficher(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            name.getScene().setRoot(root);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    @FXML
    void chooseimage(ActionEvent event) {


            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );

            File file = fileChooser.showOpenDialog(null);

            if (file != null) {
                image = file.getAbsolutePath();

                Image im = new Image(file.toURI().toString());
                imagePreview.setImage(im);
                //imagePath = file.getAbsolutePath();
            }
        }

    }




