package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import org.example.Services.animalServices;
import org.example.entities.animal;

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

}

