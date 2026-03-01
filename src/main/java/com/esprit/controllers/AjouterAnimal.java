package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import com.esprit.Services.animalServices;
import com.esprit.entities.animal;

import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private ImageView imagePreview;
    private File selectedFile; 
    private String image;
   // private String imagePath;
    animalServices service = new animalServices();


    @FXML
    public void initialize() {

        gender.getItems().addAll("MALE", "FEMALE");


    }

    @FXML
    void save(ActionEvent event) {
        try {
            // 1️⃣ Trim et récupérer les valeurs
            String nameText = name.getText().trim();
            String speciesText = species.getText().trim();
            String breedText = breed.getText().trim();
            String ageText = age.getText().trim();
            String descriptionText = description.getText().trim();
            Object genderValue = gender.getValue();



            // 2️⃣ Contrôle des champs vides
            if (nameText.isEmpty() || speciesText.isEmpty() || breedText.isEmpty() ||
                    ageText.isEmpty() || descriptionText.isEmpty() ||
                    genderValue == null ) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Champs manquants ⚠️");
                alert.setHeaderText("Attention !");
                alert.setContentText("Tous les champs doivent être remplis avant d'ajouter un animal.");
                alert.showAndWait();
                return; // stop la méthode si un champ est vide
            }

            // 3️⃣ Vérifier que l’âge est un nombre
            int ageValue;
            try {
                ageValue = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Entrée invalide ⚠️");
                alert.setHeaderText("Oops !");
                alert.setContentText("L'âge doit être un nombre entier.");
                alert.showAndWait();
                return;
            }

            // 4️⃣ Gestion de l’image
            if (selectedFile != null) {
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                try {
                    Path dir = Paths.get("images");
                    Files.createDirectories(dir);
                    Path destination = dir.resolve(fileName);
                    Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                    image = fileName; // sauvegarder seulement le nom
                } catch (IOException e) {
                    e.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur image ❌");
                    alert.setHeaderText("Impossible de copier l'image.");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                    return;
                }
            }


            // 5️⃣ Ajouter l’animal dans la base
            animal newAnimal = new animal(
                    nameText,
                    speciesText,
                    breedText,
                    ageValue,
                    animal.gender.valueOf(genderValue.toString()),
                    descriptionText,
                    animal.status.AVAILABLE,
                    image
            );
            service.ajouter(newAnimal);

            // 6️⃣ Notification créative succès
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Animal ajouté 🐾");
            alert.setHeaderText("Félicitations ! 🎉");
            alert.setContentText("L'animal a été ajouté avec succès.\n" +
                    "Il est prêt à rejoindre sa nouvelle famille 🐶💖");

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
            alert.showAndWait();

            // 7️⃣ Charger page affichage
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            name.getScene().setRoot(root);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur base de données ❌");
            alert.setHeaderText("Impossible d'ajouter l'animal 😢");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur Fichier ❌");
            alert.setHeaderText("Impossible de charger la page d'affichage.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur inattendue ❌");
            alert.setHeaderText("Quelque chose s'est mal passé 😱");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }


    @FXML
    void chooseimage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(System.getProperty("user.dir") +"Images", "*.png", "*.jpg", "*.jpeg")
        );

        selectedFile = fileChooser.showOpenDialog(null);

        // Optionnel : afficher directement un aperçu dans ton ImageView
        if (selectedFile != null) {
            Image img = new Image(selectedFile.toURI().toString());
            imagePreview.setImage(img);
        }}

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent previousPage = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            Scene scene = new Scene(previousPage);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    }




