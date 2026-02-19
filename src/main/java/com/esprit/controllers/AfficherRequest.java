package com.esprit.controllers;

import com.esprit.entities.animal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherRequest {

    @FXML
    private ListView<adoptionRequest> Listview;



    adoptionservices as = new adoptionservices();

    @FXML
    void initialize() {
        try {
            List<adoptionRequest> requests = as.afficher();
            ObservableList<adoptionRequest> observableList = FXCollections.observableList(requests);
            Listview.setItems(observableList);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent previousPage = FXMLLoader.load(getClass().getResource("/AjouterRequest.fxml"));
            Scene scene = new Scene(previousPage);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void voirdetails(ActionEvent event) {
        adoptionRequest selected = Listview.getSelectionModel().getSelectedItem();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/requestDetails.fxml"));
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


