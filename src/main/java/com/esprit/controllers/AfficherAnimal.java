package com.esprit.controllers;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherAnimal {

    @FXML
    private ListView<animal> listview;

    @FXML
    private Button btnAddAnimal;


    animalServices ps = new animalServices();




    @FXML
    public void initialize() {

        // 1️⃣ First define how each cell should look
        listview.setCellFactory(param -> new ListCell<animal>() {
            @Override
            protected void updateItem(animal a, boolean empty) {
                super.updateItem(a, empty);

                if (empty || a == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalCard.fxml"));
                        AnchorPane pane = loader.load();

                        AnimalCard controller = loader.getController();
                        controller.setData(a);

                        setGraphic(pane);
                        setPrefWidth(Control.USE_COMPUTED_SIZE);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 2️⃣ Click on Card → Open Details Page
        listview.setOnMouseClicked(event -> {
            animal selectedAnimal = listview.getSelectionModel().getSelectedItem();

            if (selectedAnimal != null) {
                try {
                    // Load FXML for the details page
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/adopanimaldetails.fxml"));
                    Parent root = loader.load();

                    // Pass the selected animal to the details controller
                    adopdetails controller = loader.getController();
                    controller.setPetData(selectedAnimal);

                    // Create a new stage for the details page
                    Stage stage = new Stage();
                    stage.setTitle("Animal Details"); // optional: set window title
                    stage.setScene(new Scene(root));

                    // Maximize the new window
                    stage.setMaximized(true);

                    // Show the details window
                    stage.show();

                    // Close the current window (afficher animal)
                    Stage currentStage = (Stage) listview.getScene().getWindow();
                    currentStage.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 2️⃣ Then load data from database
        try {
            List<animal> animals = ps.afficher();
            ObservableList<animal> observableList = FXCollections.observableList(animals);
            listview.setItems(observableList);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

        btnAddAnimal.setOnAction(e -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/AjouterAnimal.fxml"));
                btnAddAnimal.getScene().setRoot(root);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

    }




    public void removeAnimalFromList(animal animal) {
        listview.getItems().remove(animal);
    }

    @FXML
    private void openFavoriteList() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/favoriteAnimal.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle("Favorite Animals");
        stage.show();
    }

    @FXML
    void openMessageRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRequest.fxml"));
            Parent root = loader.load();

            // Create a new stage for the details page
            Stage stage = new Stage();
            stage.setTitle("Request Details"); // optional: set window title
            stage.setScene(new Scene(root));

            // Maximize the new window
            stage.setMaximized(true);

            // Show the details window
            stage.show();

            // Close the current window (afficher animal)
            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openMyAnimals(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MyAnimals.fxml"));
            listview.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

