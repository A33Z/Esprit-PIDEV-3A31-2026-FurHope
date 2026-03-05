package com.esprit.animal.controllers;

import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

import java.util.List;

public class requests extends BaseUIController {

    @FXML
    private GridPane requestsGrid;

    private final adoptionservices adoptionService = new adoptionservices();

    @Override
    protected String getViewPath() {
        return "/animal/Requests.fxml";
    }

    @FXML
    public void initialize() {
        loadRequests();
    }

    private void loadRequests() {
        try {
            int currentCompteId = Session.getCompteId();
            int currentUserId = Session.getUserId();
            System.out.println("Session Compte ID: " + currentCompteId);

            List<adoptionRequest> myRequests = adoptionService.getRequestsForMyAnimals(currentCompteId, currentUserId);
            System.out.println("Loaded requests: " + myRequests.size());

            displayRequests(myRequests);
        } catch (Exception e) {
            e.printStackTrace();
            showError(tr("request.error.loading") + ": " + e.getMessage());
        }
    }

    private void displayRequests(List<adoptionRequest> requests) {
        requestsGrid.getChildren().clear();
        int col = 0;
        int row = 0;

        try {
            for (adoptionRequest request : requests) {
                animal a = request.getAnimal();
                if (a != null) {
                    System.out.println(
                            "Request ID: " + request.getId()
                                    + ", Animal ID: " + a.getId()
                                    + ", Owner Compte ID: " + a.getOwnerCompteId()
                                    + ", Client Compte ID: " + request.getClientCompteId()
                                    + ", Status: " + request.getStatus()
                    );
                }

                FXMLLoader loader = createLoader("/animal/CardRequest.fxml");
                Parent card = loader.load();

                CardRequest controller = loader.getController();
                controller.setRefreshCallback(this::loadRequests);
                controller.setData(request);

                requestsGrid.add(card, col, row);
                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(tr("request.error.display") + ": " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tr("common.error"));
        alert.setHeaderText(tr("common.oops"));
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}

