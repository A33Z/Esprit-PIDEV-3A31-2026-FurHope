package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.entities.adoptionRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherRequest extends BaseUIController {

    @FXML
    private ListView<adoptionRequest> Listview;

    adoptionservices as = new adoptionservices();

    @Override
    protected String getViewPath() {
        return "/animal/AfficherRequest.fxml";
    }

    @FXML
    void initialize() {

        Listview.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(adoptionRequest request, boolean empty) {
                super.updateItem(request, empty);

                if (empty || request == null) {
                    setGraphic(null);
                    return;
                }

                try {
                    FXMLLoader loader = createLoader("/animal/Myrequestcard.fxml");
                    Parent root = loader.load();

                    MyrequestCard controller = loader.getController();
                    controller.setData(request, getIndex());

                    setGraphic(root);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        loadRequests();
    }

    public void loadRequests() {

        try {
            int sessionCompteId = com.esprit.animal.utils.Session.getCompteId();
            int sessionUserId = com.esprit.animal.utils.Session.getUserId();
            List<adoptionRequest> myRequests = as.getRequestsByClientSession(sessionCompteId, sessionUserId);

            Listview.getItems().setAll(FXCollections.observableList(myRequests));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent previousPage = loadView("/animal/AfficherAnimal.fxml");

            // Create a new stage for the details page
            Stage stage = new Stage();
            StageSceneHelper.setScene(stage, previousPage);
            // Maximize the new window
            stage.setMaximized(true);
            stage.show();

            // Close the current window (afficher animal)
            Stage currentStage = (Stage) Listview.getScene().getWindow();
            currentStage.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void voirdetails(ActionEvent event) {
        adoptionRequest selected = Listview.getSelectionModel().getSelectedItem();
        try {
            FXMLLoader loader = createLoader("/animal/RequestDetails.fxml");
            Parent root = loader.load();

            // envoyer l'animal au controller dÃƒÂ©tails
            requestdetails controller = loader.getController();
            controller.setRequest(selected);

            Stage stage = new Stage();
            StageSceneHelper.setScene(stage, root);
            stage.show();

            // fermer la fenÃƒÂªtre actuelle
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void removeDemandeFromList(adoptionRequest request) {
        Listview.getItems().remove(request);
    }


}





