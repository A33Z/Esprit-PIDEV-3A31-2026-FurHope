package com.esprit.controllers;

import com.esprit.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterRequest {
    @FXML
    private Label animalNameLabel, animalSpeciesLabel, animalBreedLabel, animalAgeGenderLabel;

    @FXML
    private ImageView animalImageView;

    @FXML
    private Label clientNameLabel, clientEmailLabel, clientPhoneLabel;

    @FXML
    private TextArea message;

    @FXML
    private Button sendButton;

    private int animalId;

    adoptionservices serv = new adoptionservices();

    // 1️⃣ Méthode pour pré-remplir l’animal
    public void setAnimalInfo(int id, String name, String species, String breed, String ageGender, Image image) {
        this.animalId = id;

        animalNameLabel.setText("Name: " + name);
        animalSpeciesLabel.setText("Species: " + species);
        animalBreedLabel.setText("Breed: " + breed);
        animalAgeGenderLabel.setText(ageGender);

        if (image != null) {
            animalImageView.setImage(image);
        }
    }

    // 2️⃣ Méthode pour pré-remplir client depuis Session
    public void setClientInfoFromSession() {
        clientNameLabel.setText("Name: " + Session.getUserName());
        clientEmailLabel.setText("Email: " + Session.getUserEmail());
        clientPhoneLabel.setText("Phone: " + Session.getUserPhone());
    }


    @FXML
    void Envoyer(ActionEvent event) {
        try {
            String msg = message.getText().trim();


            // 2️⃣ Contrôle des champs vides
            if (msg.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Message missing");
                alert.setContentText("Please write a message before sending!");
                alert.showAndWait();
                return;
            }

            // 3️⃣ Conversion en nombres
            int clientId = Session.getUserId();

            // 4️⃣ Créer et ajouter la demande
            adoptionRequest request = new adoptionRequest(animalId, clientId, msg, Session.getUserPhone()+"", "dummy address", adoptionRequest.status.PENDING);
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
