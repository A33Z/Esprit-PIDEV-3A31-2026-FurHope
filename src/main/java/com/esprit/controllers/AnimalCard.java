package com.esprit.controllers;

import com.esprit.entities.animal;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnimalCard {


    @FXML
    private ImageView animalImage;

    @FXML
        private Label nameLabel;

    @FXML
        private Label speciesLabel;
    @FXML
    private Label ageLabel;

    @FXML
    private Button favoriteButton;

    private animal currentAnimal;

    private boolean isFavorite = false;



    // ✅ Set animal data in card
    public void setData(animal animal) {
        this.currentAnimal = animal;

        nameLabel.setText(animal.getName());
        speciesLabel.setText(animal.getSpecies());
        ageLabel.setText(String.valueOf(animal.getAge()));

        if (animal.getImage() != null) {
            File file = new File("images/" + animal.getImage());
            if (file.exists()) {
                animalImage.setImage(new Image(file.toURI().toString()));
            }
        }

        // check if already favorite
        if (favoriteAnimals.contains(animal)) {
            isFavorite = true;
            favoriteButton.setText("♡");
            favoriteButton.setStyle("-fx-font-size: 24px; -fx-text-fill: red; -fx-background-color: transparent;");
        } else {
            isFavorite = false;
            favoriteButton.setText("♡");
            favoriteButton.setStyle("-fx-font-size: 24px; -fx-text-fill: gray; -fx-background-color: transparent;");
        }
    }



    // pour que ma favoritelist refresh instantly
    private favoritecontroller parentController;

    public void setParentController(favoritecontroller controller) {
        this.parentController = controller;
    }

    // Static list to store favorite animals
    private static final List<animal> favoriteAnimals = new ArrayList<>();

    public static List<animal> getFavoriteAnimals() {
        return favoriteAnimals;
    }
    public static void addFavorite(animal a) {
        if (!favoriteAnimals.contains(a)) {
            favoriteAnimals.add(a);
        }
    }
    // ✅ Toggle favorite
    @FXML
    private void handleFavoriteButton(ActionEvent event) {

        isFavorite = !isFavorite;

        if (isFavorite) {
            favoriteButton.setText("♡");
            favoriteButton.setStyle("-fx-font-size: 24px; -fx-text-fill: red; -fx-background-color: transparent;");
            if (!favoriteAnimals.contains(currentAnimal)) {
                addFavorite(currentAnimal);
            }
        } else {
            favoriteButton.setText("♡");
            favoriteButton.setStyle("-fx-font-size: 24px; -fx-text-fill: gray; -fx-background-color: transparent;");
            favoriteAnimals.remove(currentAnimal);
            // notify parent controller to refresh grid
            if (parentController != null) {
                parentController.loadFavorites();
            }
        }
    }


    // ✅ Open Details Page (YOUR FUNCTION — cleaned)
    @FXML
    private void voirdetails(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/animalDetails.fxml"));
            Parent root = loader.load();

            AnimalDetails controller = loader.getController();
            controller.setAnimal(currentAnimal);

            // 🔹 Get current stage (the one containing this button)
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 🔹 Create new stage for details
            Stage detailsStage = new Stage();
            detailsStage.setTitle("Animal Details");
            detailsStage.setScene(new Scene(root));

            detailsStage.setMaximized(true); // REAL full screen
            detailsStage.show();

            // 🔹 Close current window
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


