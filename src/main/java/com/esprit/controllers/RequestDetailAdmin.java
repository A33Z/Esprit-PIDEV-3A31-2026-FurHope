package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.Services.animalServices;
import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class RequestDetailAdmin {

    @FXML private Label animalNameLabel;
    @FXML private Label animalSpeciesLabel;
    @FXML private Label animalBreedLabel;
    @FXML private Label animalAgeGenderLabel;

    @FXML private Label requesterNameLabel;
    @FXML private Label requesterEmailLabel;
    @FXML private Label requesterPhoneLabel;

    @FXML private Label statusLabel;

    @FXML private Button approveButton;
    @FXML private Button declineButton;

    private adoptionRequest currentRequest;

    private adoptionservices adoptionService = new adoptionservices();
    private animalServices animalService = new animalServices();

    public void setRequest(adoptionRequest request) {
        this.currentRequest = request;

        animal a = request.getAnimal(); // ⚡ directement depuis la demande

        if(a != null){
            animalNameLabel.setText(a.getName());
            animalSpeciesLabel.setText(a.getSpecies());
            animalBreedLabel.setText(a.getBreed());
            animalAgeGenderLabel.setText(a.getAge() + " - " + a.getGender());
        }

        requesterNameLabel.setText("Client ID: " + request.getClient_id());
        requesterEmailLabel.setText("-");
        requesterPhoneLabel.setText("-");

        statusLabel.setText("Status: " + request.getStatus());

        approveButton.setOnAction(e -> updateStatus("APPROVED"));
        declineButton.setOnAction(e -> updateStatus("DECLINED"));
    }

    @FXML
    private void approve() {
        updateStatus("APPROVED");
    }

    @FXML
    private void decline() {
        updateStatus("DECLINED");
    }

    private void updateStatus(String newStatus) {

        try {
            currentRequest.setStatus(
                    com.esprit.entities.adoptionRequest.status.valueOf(newStatus)
            );

            adoptionService.modifier(currentRequest);

            statusLabel.setText("Status: " + newStatus);

            Stage stage = (Stage) approveButton.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}