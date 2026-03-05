package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.entities.adoptionRequest;
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
            return "/animal/RequestDetails.fxml";
        }

        @Override
        protected String getBackViewPath() {
            return "/animal/AfficherRequest.fxml";
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
                // VÃƒÂ©rifier que la requÃƒÂªte sÃƒÂ©lectionnÃƒÂ©e n'est pas nulle
                if (requestSelected == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Aucune sÃƒÂ©lection Ã¢Å¡Â Ã¯Â¸Â");
                    alert.setHeaderText("Erreur !");
                    alert.setContentText("Aucune demande sÃƒÂ©lectionnÃƒÂ©e pour modifier.");
                    alert.showAndWait();
                    return;
                }

                // Fermer la fenÃƒÂªtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();

                // Charger le formulaire de modification
                FXMLLoader loader = createLoader("/animal/RequestUpdate.fxml");
                Parent root = loader.load();

                // Passer la demande sÃƒÂ©lectionnÃƒÂ©e et la listview au controller
                requestupdate controller = loader.getController();
                controller.setRequest(requestSelected);
                controller.setListView(listView);

                Stage stage = new Stage();
                StageSceneHelper.setScene(stage, root);
                stage.setTitle("Modifier demande");

                // Maximiser le stage
                stage.setMaximized(true);

                stage.show();

                // Notification crÃƒÂ©ative
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Modifier demande Ã¢Å“ÂÃ¯Â¸Â");
                alert.setHeaderText("Ouverture rÃƒÂ©ussie Ã¢Å“â€¦");
                alert.setContentText("Le formulaire de modification s'est ouvert avec succÃƒÂ¨s.");
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Fichier Ã¢ÂÅ’");
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
                    alert.setTitle("Aucune sÃƒÂ©lection Ã¢Å¡Â Ã¯Â¸Â");
                    alert.setHeaderText("Erreur !");
                    alert.setContentText("Aucune demande sÃƒÂ©lectionnÃƒÂ©e pour supprimer.");
                    alert.showAndWait();
                    return;
                }

                adoptionservices service = new adoptionservices();
                service.supprimer(requestSelected.getId());

                // Notification succÃƒÂ¨s
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Suppression Ã¢Å“â€¦");
                alert.setHeaderText("Demande supprimÃƒÂ©e");
                alert.setContentText("La demande a ÃƒÂ©tÃƒÂ© supprimÃƒÂ©e avec succÃƒÂ¨s.");
                alert.showAndWait();

                // Fermer la fenÃƒÂªtre Details
                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                currentStage.close();

                // Recharge la liste
                FXMLLoader loader = createLoader("/animal/AfficherRequest.fxml");
                Parent root = loader.load();

                AfficherRequest controller = loader.getController();
                controller.removeDemandeFromList(requestSelected);

                Stage listStage = new Stage();
                StageSceneHelper.setScene(listStage, root);
                listStage.setMaximized(true); // maximise la fenÃƒÂªtre de liste
                listStage.show();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Ã¢ÂÅ’");
                alert.setHeaderText("Impossible de supprimer la demande");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
}



