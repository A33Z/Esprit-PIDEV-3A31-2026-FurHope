package com.esprit.animal.controllers;

import com.esprit.animal.Services.AIDescriptionService;
import com.esprit.animal.Services.AutoRecognitionService;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.entities.animal;
import com.esprit.animal.utils.Session;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class AjouterAnimal extends BaseUIController {

    @FXML
    private TextField age;
    @FXML
    private TextField breed;
    @FXML
    private TextArea description;
    @FXML
    private ComboBox<String> gender;
    @FXML
    private TextField name;
    @FXML
    private TextField species;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Label confidenceLabel;
    @FXML
    private Label imagePathLabel;

    private File selectedFile;
    private String image;
    private AutoRecognitionService.AnimalRecognitionResult lastRecognitionResult;
    private final animalServices service = new animalServices();
    private final AIDescriptionService aiDescriptionService = new AIDescriptionService();

    @Override
    protected String getViewPath() {
        return "/animal/AjouterAnimal.fxml";
    }

    @FXML
    public void initialize() {
        gender.getItems().addAll("MALE", "FEMALE");
        confidenceLabel.setText("");
    }

    @FXML
    void chooseimage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            imagePathLabel.setText("OK " + selectedFile.getName());
            Image img = new Image(selectedFile.toURI().toString());
            imagePreview.setImage(img);
        }
    }

    @FXML
    void autoRecognize(ActionEvent event) {
        if (selectedFile == null) {
            showAlert("Veuillez d'abord selectionner une image.", Alert.AlertType.WARNING);
            return;
        }

        lastRecognitionResult = AutoRecognitionService.analyzeImage(selectedFile);

        species.setText(lastRecognitionResult.getSpecies());
        breed.setText(lastRecognitionResult.getBreed());

        int confidencePercent = (int) (lastRecognitionResult.getConfidence() * 100);
        confidenceLabel.setText(String.format("Confiance : %d%%", confidencePercent));

        if (confidencePercent >= 85) {
            confidenceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else if (confidencePercent >= 70) {
            confidenceLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            confidenceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        showAlert(
                "Reconnaissance reussie\n\n" + lastRecognitionResult + "\n\nVerifiez et completez avant de valider.",
                Alert.AlertType.INFORMATION
        );
    }

    @FXML
    void generateDescription(ActionEvent event) {
        String nameText = name.getText().trim();
        String speciesText = species.getText().trim();
        String breedText = breed.getText().trim();
        String ageText = age.getText().trim();
        Object genderValue = gender.getValue();

        if (nameText.isEmpty() || speciesText.isEmpty() || breedText.isEmpty() || ageText.isEmpty() || genderValue == null) {
            showAlert("Remplissez Nom, Age, Espece, Race et Sexe pour generer une description.", Alert.AlertType.WARNING);
            return;
        }

        int ageValue;
        try {
            ageValue = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showAlert("L'age doit etre un nombre entier pour generer la description.", Alert.AlertType.WARNING);
            return;
        }

        String generatedDescription = aiDescriptionService.generateDescription(
                nameText,
                speciesText,
                breedText,
                ageValue,
                animal.gender.valueOf(genderValue.toString())
        );

        description.setText(generatedDescription);
    }

    @FXML
    void save(ActionEvent event) {
        try {
            String nameText = name.getText().trim();
            String speciesText = species.getText().trim();
            String breedText = breed.getText().trim();
            String ageText = age.getText().trim();
            String descriptionText = description.getText().trim();
            Object genderValue = gender.getValue();

            if (nameText.isEmpty() || speciesText.isEmpty() || breedText.isEmpty() ||
                    ageText.isEmpty() || genderValue == null) {
                showAlert("Nom, Age, Espece, Race et Sexe sont obligatoires.", Alert.AlertType.WARNING);
                return;
            }

            int ageValue;
            try {
                ageValue = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                showAlert("L'age doit etre un nombre entier.", Alert.AlertType.WARNING);
                return;
            }

            if (selectedFile != null) {
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                try {
                    Path dir = Paths.get("images");
                    Files.createDirectories(dir);
                    Path destination = dir.resolve(fileName);
                    Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                    image = fileName;
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Erreur lors de la sauvegarde de l'image.", Alert.AlertType.ERROR);
                    return;
                }
            }

            int ownerCompteId = Session.getCompteId();

            if (descriptionText.isEmpty()) {
                descriptionText = aiDescriptionService.generateDescription(
                        nameText,
                        speciesText,
                        breedText,
                        ageValue,
                        animal.gender.valueOf(genderValue.toString())
                );
                description.setText(descriptionText);
            }

            animal newAnimal = new animal(
                    nameText,
                    speciesText,
                    breedText,
                    ageValue,
                    animal.gender.valueOf(genderValue.toString()),
                    descriptionText,
                    animal.status.AVAILABLE,
                    image,
                    ownerCompteId
            );

            service.ajouter(newAnimal);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Animal ajoute avec succes");
            alert.setHeaderText("Operation terminee");
            alert.setContentText(nameText + " est pret a trouver sa nouvelle famille.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/animal/style.css").toExternalForm());
            alert.showAndWait();

            Parent root = loadView("/animal/AfficherAnimal.fxml");
            name.getScene().setRoot(root);

        } catch (SQLException e) {
            showAlert("Erreur base de donnees : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (IOException e) {
            showAlert("Erreur fichier : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void handleRetour(Event event) {
        navigate(event, "/animal/AfficherAnimal.fxml");
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.INFORMATION ? "Info" :
                type == Alert.AlertType.WARNING ? "Attention" : "Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

