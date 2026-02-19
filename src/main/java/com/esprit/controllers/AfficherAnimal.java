package com.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import com.esprit.Services.animalServices;
import com.esprit.entities.animal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherAnimal {

    @FXML
    private ListView<animal> listview;

    animalServices ps = new animalServices();

    @FXML
    void initialize() {
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
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent previousPage = FXMLLoader.load(getClass().getResource("/AjouterAnimal.fxml"));
            Scene scene = new Scene(previousPage);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @FXML
    void voirdetails(ActionEvent event) {
        animal selected = listview.getSelectionModel().getSelectedItem();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/animalDetails.fxml"));
            Parent root = loader.load();

            // envoyer l'animal au controller détails
            AnimalDetails controller = loader.getController();
            controller.setAnimal(selected);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            // fermer la fenêtre actuelle
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void removeAnimalFromList(animal animal) {
        listview.getItems().remove(animal);
    }
}

