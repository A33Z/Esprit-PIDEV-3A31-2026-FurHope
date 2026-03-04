package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;

    public class requestdetails extends BaseUIController {
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

        @Override
        protected String getViewPath() {
            return "/RequestDetails.fxml";
        }

        @Override
        protected String getBackViewPath() {
            return "/AfficherRequest.fxml";
        }

        public void setRequest(adoptionRequest request) {
            this.requestSelected = request;


            animalidlabel.setText(String.valueOf(request.getAnimal_id()));
            clientidlabel.setText(String.valueOf(request.getClientCompteId()));
            messagelabel.setText(request.getMessage());
            phonelabel.setText(request.getPhone());
            addresslabel.setText(request.getAddress());
            statusLabel.setText(request.getStatus().toString());


        }


        @FXML
        void handlemodifier(ActionEvent event) {
            try {
                // Vérifier que la requête sélectionnée n'est pas nulle
                if (requestSelected == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Aucune sélection ⚠️");
                    alert.setHeaderText("Erreur !");
                    alert.setContentText("Aucune demande sélectionnée pour modifier.");
                    alert.showAndWait();
                    return;
                }

                // Fermer la fenêtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();

                // Charger le formulaire de modification
                FXMLLoader loader = createLoader("/RequestUpdate.fxml");
                Parent root = loader.load();

                // Passer la demande sélectionnée et la listview au controller
                requestupdate controller = loader.getController();
                controller.setRequest(requestSelected);
                controller.setListView(listView);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier demande");

                // Maximiser le stage
                stage.setMaximized(true);

                stage.show();

                // Notification créative
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Modifier demande ✏️");
                alert.setHeaderText("Ouverture réussie ✅");
                alert.setContentText("Le formulaire de modification s'est ouvert avec succès.");
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Fichier ❌");
                alert.setHeaderText("Impossible d'ouvrir le formulaire");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }


        @FXML
        void handlesupprimer(ActionEvent event) {
            try {
                if (requestSelected == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Aucune sélection ⚠️");
                    alert.setHeaderText("Erreur !");
                    alert.setContentText("Aucune demande sélectionnée pour supprimer.");
                    alert.showAndWait();
                    return;
                }

                adoptionservices service = new adoptionservices();
                service.supprimer(requestSelected.getId());

                // Notification succès
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Suppression ✅");
                alert.setHeaderText("Demande supprimée");
                alert.setContentText("La demande a été supprimée avec succès.");
                alert.showAndWait();

                // Fermer la fenêtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();

                // Recharge la liste
                FXMLLoader loader = createLoader("/AfficherRequest.fxml");
                Parent root = loader.load();

                AfficherRequest controller = loader.getController();
                controller.removeDemandeFromList(requestSelected);

                Stage listStage = new Stage();
                listStage.setScene(new Scene(root));
                listStage.setMaximized(true); // maximise la fenêtre de liste
                listStage.show();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur ❌");
                alert.setHeaderText("Impossible de supprimer la demande");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
}
