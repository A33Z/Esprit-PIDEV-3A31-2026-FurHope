package com.esprit.controllers;

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
import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherRequest extends BaseUIController {

    @FXML
    private ListView<adoptionRequest> Listview;

    adoptionservices as = new adoptionservices();

    @Override
    protected String getViewPath() {
        return "/AfficherRequest.fxml";
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
                    FXMLLoader loader = createLoader("/Myrequestcard.fxml");
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
            List<adoptionRequest> allRequests = as.afficher();

            // Filtrer uniquement les demandes du current user
            List<adoptionRequest> myRequests = allRequests.stream()
                    .filter(r -> r.getClientCompteId() == com.esprit.utils.Session.getCompteId())
                    .toList();

            Listview.getItems().setAll(FXCollections.observableList(myRequests));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent previousPage = loadView("/AfficherAnimal.fxml");

            // Create a new stage for the details page
            Stage stage = new Stage();
            stage.setScene(new Scene(previousPage));
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
            FXMLLoader loader = createLoader("/RequestDetails.fxml");
            Parent root = loader.load();

            // envoyer l'animal au controller détails
            requestdetails controller = loader.getController();
            controller.setRequest(selected);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            // fermer la fenêtre actuelle
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


