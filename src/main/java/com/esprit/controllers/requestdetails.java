package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.Services.animalServices;
import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

    public class requestdetails {
        @FXML
        private Label animalidlabel ;

        @FXML
        private Label clientidlabel;

        @FXML
        private Label messagelabel;

        @FXML
        private Label phonelabel;


        @FXML
        private Label addresslabel;

        @FXML
        private Label statusLabel;

        private adoptionRequest requestSelected;
        private ListView<adoptionRequest> listView;

        public void setRequest(adoptionRequest request) {
            this.requestSelected = request;


            animalidlabel.setText(String.valueOf(request.getAnimal_id()));
            clientidlabel.setText(String.valueOf(request.getClient_id()));
            messagelabel.setText(request.getMessage());
            phonelabel.setText(request.getPhone());
            addresslabel.setText(request.getAddress());
            statusLabel.setText(request.getStatus().toString());


        }


        @FXML
        void handlemodifier(ActionEvent event) {

            try {
                // 1. Fermer la fenêtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();


                FXMLLoader loader = new FXMLLoader(getClass().getResource("/RequestUpdate.fxml"));
                Parent root = loader.load();


                requestupdate controller = loader.getController();
                controller.setRequest(requestSelected);      // passer l'animal sélectionné
                controller.setListView(listView);        // passer la table pour refresh

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier demande");
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        @FXML
        void handlesupprimer(ActionEvent event) {
            try {
                adoptionservices service = new adoptionservices();
                service.supprimer(requestSelected.getId());

                // fermer la fenêtre
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();


                // 3. Ouvrir la fenêtre d'affichage des animaux
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRequest.fxml"));
                Parent root = loader.load();

                // Récupérer le controller de la liste pour mettre à jour la TableView/ListView
                AfficherRequest controller = loader.getController();
                controller.removeDemandeFromList(requestSelected); // méthode à créer dans le controller

                Stage listStage = new Stage();
                listStage.setScene(new Scene(root));
                listStage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

}
