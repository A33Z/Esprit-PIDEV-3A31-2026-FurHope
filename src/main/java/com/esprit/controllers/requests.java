package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import com.esprit.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class requests {

    @FXML
    private GridPane requestsGrid;

    private adoptionservices adoptionService = new adoptionservices();

    @FXML
    public void initialize() {
        try {
            int currentUserId = Session.getUserId();
            System.out.println("Session User ID: " + currentUserId);

            // récupère uniquement les demandes de MES animaux
            List<adoptionRequest> myRequests = adoptionService.getRequestsForMyAnimals(currentUserId);
            System.out.println("Nombre de requests récupérées: " + myRequests.size());

            if (myRequests.isEmpty()) {
                System.out.println("Aucune demande pour les animaux de cet utilisateur.");
            }

            displayRequests(myRequests);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des demandes: " + e.getMessage());
        }
    }

    private void displayRequests(List<adoptionRequest> requests) {

        requestsGrid.getChildren().clear();
        int col = 0;
        int row = 0;

        try {
            for (adoptionRequest request : requests) {
                // 🔹 Log pour debug
                animal a = request.getAnimal();
                System.out.println("Request ID: " + request.getId() +
                        ", Animal ID: " + a.getId() +
                        ", Owner ID: " + a.getOwnerid() +
                        ", Client ID: " + request.getClient_id() +
                        ", Status: " + request.getStatus());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/CardRequest.fxml"));
                AnchorPane card = loader.load();

                CardRequest controller = loader.getController();
                controller.setData(request); // assure-toi que CardRequest a bien cette méthode

                requestsGrid.add(card, col, row);

                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'affichage des demandes: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Oups !");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}