package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.entities.animal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class favoritecontroller extends BaseUIController {

    @FXML
    private GridPane favoritesGrid;

    @FXML
    private Label emptyLabel;

    @FXML
    private Button backButton;

    @Override
    protected String getViewPath() {
        return "/animal/favoriteAnimal.fxml";
    }

    @FXML
    public void initialize() {
        loadFavorites();
    }

    public void loadFavorites() {
        favoritesGrid.getChildren().clear();

        List<animal> favorites = AnimalCard.getFavoriteAnimals();
        System.out.println("Favorite count: " + favorites.size());

        if (favorites.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setText("No favorite animals yet.\n\nGo back to add some.");
            return;
        }

        emptyLabel.setVisible(false);

        int col = 0;
        int row = 0;

        for (animal a : favorites) {
            try {
                FXMLLoader loader = createLoader("/animal/AnimalCard.fxml");
                AnchorPane card = loader.load();

                AnimalCard controller = loader.getController();
                controller.setData(a);
                controller.setRefreshCallback(this::refreshFavorites);

                favoritesGrid.add(card, col, row);

                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }

            } catch (IOException e) {
                System.err.println("Error loading card: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleBack() {
        try {
            Parent root = loadView("/animal/AfficherAnimal.fxml");
            Stage stage = (Stage) backButton.getScene().getWindow();
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshFavorites() {
        loadFavorites();
    }

    @FXML
    void clearAllFavorites() {
        List<animal> favorites = AnimalCard.getFavoriteAnimals();

        if (favorites.isEmpty()) {
            showAlert("Info", "No favorites to clear.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to clear all favorites?");

        if (confirmAlert.showAndWait().orElse(null) == ButtonType.OK) {
            AnimalCard.clearFavorites();
            loadFavorites();
            showAlert("Success", "All favorites were removed.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    void showStats() {
        try {
            FXMLLoader loader = createLoader("/animal/StatisticsDashboard.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Statistics Dashboard");
            StageSceneHelper.setScene(stage, root);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error opening statistics dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open statistics dashboard.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



