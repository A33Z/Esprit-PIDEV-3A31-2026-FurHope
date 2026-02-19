package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterRequest {
    @FXML
    private TextField address;

    @FXML
    private TextField animal_id;

    @FXML
    private TextField client_id;

    @FXML
    private TextField message;

    @FXML
    private TextField phone;

    @FXML
    private ComboBox<String> status;

    adoptionservices serv = new adoptionservices();

    @FXML
    public void initialize() {

        status.getItems().addAll("PENDING", "APPROVED", "REJECTED");

    }

    @FXML
    void Envoyer(ActionEvent event) {
        try {
            serv.ajouter(new adoptionRequest(Integer.parseInt(animal_id.getText()),Integer.parseInt(client_id.getText()),message.getText(), address.getText(), phone.getText(), adoptionRequest.status.valueOf(status.getValue())));

            // 2️⃣ Charger page affichage
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherRequest.fxml"));
            animal_id.getScene().setRoot(root);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
    } catch (IOException e) {
        System.out.println(e.getMessage());
    }}


@FXML
    void Afficher(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherRequest.fxml"));
            animal_id.getScene().setRoot(root);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

}
