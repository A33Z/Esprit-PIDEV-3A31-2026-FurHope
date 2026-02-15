package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import org.example.Services.animalServices;
import org.example.entities.animal;

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
    private TextField gender;

    @FXML
    private TextField name;

    @FXML
    private TextField species;

    @FXML
    private TextField status;

     animalServices service = new animalServices();

    @FXML
    void save(ActionEvent event) {
        try {
            service.ajouter(new animal(name.getText(),species.getText(),breed.getText(),Integer.parseInt(age.getText()), animal.gender.valueOf(gender.getText()), description.getText(), animal.status.valueOf(status.getText())));
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setContentText(e.getMessage());
            alert.showAndWait();        }
    }

    @FXML
    void afficher(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));
            name.getScene().setRoot(root);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
