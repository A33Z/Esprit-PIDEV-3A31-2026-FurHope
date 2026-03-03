package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.Services.AutoRecognitionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
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

    @FXML private TextField age;
    @FXML private TextField breed;
    @FXML private TextField description;
    @FXML private ComboBox<String> gender;
    @FXML private TextField name;
    @FXML private TextField species;
    @FXML private ImageView imagePreview;
    @FXML private Label confidenceLabel;
    @FXML private Label imagePathLabel;

    private User currentUser;
    private File selectedFile;
    private String image;
    private AutoRecognitionService.AnimalRecognitionResult lastRecognitionResult;
    private animalServices service = new animalServices();

    @FXML
    public void initialize() {
        gender.getItems().addAll("MALE", "FEMALE");
        confidenceLabel.setText("");
    }

    /**
     * 📸 Sélectionner une image
     */
    @FXML
    void chooseimage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            imagePathLabel.setText("✓ " + selectedFile.getName());
            Image img = new Image(selectedFile.toURI().toString());
            imagePreview.setImage(img);
        }
    }

    /**
     * 🤖 RECONNAISSANCE AUTOMATIQUE - Analyze l'image et remplit les champs
     */
    @FXML
    void autoRecognize(ActionEvent event) {
        if (selectedFile == null) {
            showAlert("⚠️ Veuillez d'abord sélectionner une image", Alert.AlertType.WARNING);
            return;
        }

        // Analyser l'image
        lastRecognitionResult = AutoRecognitionService.analyzeImage(selectedFile);

        // Remplir les champs
        species.setText(lastRecognitionResult.getSpecies());
        breed.setText(lastRecognitionResult.getBreed());

        // Afficher la confiance avec couleur
        int confidencePercent = (int) (lastRecognitionResult.getConfidence() * 100);
        confidenceLabel.setText(String.format("✓ Confiance : %d%%", confidencePercent));

        if (confidencePercent >= 85) {
            confidenceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else if (confidencePercent >= 70) {
            confidenceLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            confidenceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        showAlert(
                "✓ Reconnaissance réussie\n\n" + lastRecognitionResult.toString() +
                        "\n\nVérifiez et complétez avant de valider.",
                Alert.AlertType.INFORMATION
        );
    }

    /**
     * 💾 Sauvegarder l'animal
     */
    @FXML
    void save(ActionEvent event) {
        try {
            // Récupérer et valider
            String nameText = name.getText().trim();
            String speciesText = species.getText().trim();
            String breedText = breed.getText().trim();
            String ageText = age.getText().trim();
            String descriptionText = description.getText().trim();
            Object genderValue = gender.getValue();

            if (nameText.isEmpty() || speciesText.isEmpty() || breedText.isEmpty() ||
                    ageText.isEmpty() || descriptionText.isEmpty() || genderValue == null) {
                showAlert("Tous les champs doivent être remplis ⚠️", Alert.AlertType.WARNING);
                return;
            }

            int ageValue;
            try {
                ageValue = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                showAlert("L'âge doit être un nombre entier ⚠️", Alert.AlertType.WARNING);
                return;
            }

            // Gestion de l'image
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
                    showAlert("Erreur lors de la sauvegarde de l'image ❌", Alert.AlertType.ERROR);
                    return;
                }
            }

            int ownerId = com.esprit.utils.Session.getUserId();

            // Créer l'animal
            animal newAnimal = new animal(
                    nameText,
                    speciesText,
                    breedText,
                    ageValue,
                    animal.gender.valueOf(genderValue.toString()),
                    descriptionText,
                    animal.status.AVAILABLE,  // ✅ Status AVAILABLE par défaut
                    image,
                    ownerId
            );

            service.ajouter(newAnimal);

            // Succès
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🐾 Animal ajouté avec succès !");
            alert.setHeaderText("Félicitations ! 🎉");
            alert.setContentText(nameText + " est prêt à trouver sa nouvelle famille !");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            alert.showAndWait();

            // Retourner à la liste
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            name.getScene().setRoot(root);

        } catch (SQLException e) {
            showAlert("Erreur base de données : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (IOException e) {
            showAlert("Erreur fichier : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

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

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.INFORMATION ? "ℹ️ Info" :
                type == Alert.AlertType.WARNING ? "⚠️ Attention" : "❌ Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}