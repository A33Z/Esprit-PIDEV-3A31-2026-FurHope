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
import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterRequest {
    @FXML
    private TextField address;

    @FXML
    private TextField animalNameLabel;

    @FXML
    private TextField animal_id;

    @FXML
    private TextField client_id;

    @FXML
    private TextField message;

    @FXML
    private TextField phone;

    private int animalId; // store internally


    adoptionservices serv = new adoptionservices();


    // Method to set animal info when opening this page
    public void setAnimalInfo(int id, String name) {
        this.animalId = id;
        animal_id.setText(String.valueOf(id));
        animal_id.setEditable(false); // cannot modify
        animalNameLabel.setText(name);
        animalNameLabel.setEditable(false); // cannot modify
    }

    @FXML
    void Envoyer(ActionEvent event) {
        try {
            // 1️⃣ Trim et récupérer les valeurs
            String clientText = client_id.getText().trim();
            String msg = message.getText().trim();
            String addr = address.getText().trim();
            String phoneNum = phone.getText().trim();

            // 2️⃣ Contrôle des champs vides
            if ( clientText.isEmpty() || msg.isEmpty() ||
                    addr.isEmpty() || phoneNum.isEmpty()) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Champs manquants ⚠️");
                alert.setHeaderText("Attention !");
                alert.setContentText("Tous les champs doivent être remplis avant d'envoyer la demande.");
                alert.showAndWait();
                return; // stop la méthode si un champ est vide
            }

            // 3️⃣ Conversion en nombres
            int clientId = Integer.parseInt(clientText);

            // 4️⃣ Créer et ajouter la demande
            adoptionRequest request = new adoptionRequest(animalId, clientId, msg, addr, phoneNum, adoptionRequest.status.PENDING);
            serv.ajouter(request);

            // 5️⃣ Notification créative
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Adoption Request Submitted 🌟");
            alert.setHeaderText("Hooray! 🎉");
            alert.setContentText("Votre demande a été envoyée avec succès ! 🐾\n" +
                    "Nos amis poilus ont hâte de vous rencontrer 😺💖");

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");

            alert.showAndWait();

            // 6️⃣ GO BACK TO AfficherAnimal.fxml
            Parent root = FXMLLoader.load(
                    getClass().getResource("/AfficherAnimal.fxml")
            );

            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(false);  // first turn off maximize
            stage.setMaximized(true);  // maximize new stage

            stage.show();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Entrée invalide ⚠️");
            alert.setHeaderText("Oops !");
            alert.setContentText("Les champs Animal ID et Client ID doivent être des nombres.");
            alert.showAndWait();

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur base de données ❌");
            alert.setHeaderText("Impossible d'enregistrer la demande 😢");
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

}
