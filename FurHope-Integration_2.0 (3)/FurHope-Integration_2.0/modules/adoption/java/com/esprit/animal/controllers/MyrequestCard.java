package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.i18n.LanguageManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MyrequestCard {


        @FXML
        private VBox cardRoot;

        @FXML
        private Label titleLabel;

        @FXML
        private Label animalLabel;

        @FXML
        private Label clientLabel;

        @FXML
        private Label statusLabel;

        private adoptionRequest currentRequest;

        public void setData(adoptionRequest request, int index) {

            this.currentRequest = request;

            titleLabel.setText("Request " + (index + 1));

            animalLabel.setText("Animal ID: " + request.getAnimal_id());
            clientLabel.setText("Client Compte ID: " + request.getClientCompteId());
            statusLabel.setText("Status: " + request.getStatus());

            cardRoot.setOnMouseClicked(e -> openDetails());
        }

        private void openDetails() {

            try {

                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource("/animal/RequestDetails.fxml"), LanguageManager.getBundle());

                Parent root = loader.load();

                requestdetails controller = loader.getController();

                controller.setRequest(currentRequest);

                // Get current stage and close it
                Stage currentStage = (Stage) cardRoot.getScene().getWindow(); // replace someNode with a node from current scene
                currentStage.close();

                // Create a new stage for the details page
                Stage stage = new Stage();
                StageSceneHelper.setScene(stage, root);
                // Maximize the new window
                stage.setMaximized(true);
                stage.show();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




