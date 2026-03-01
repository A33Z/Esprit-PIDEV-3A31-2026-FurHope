package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.entities.adoptionRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class requestupdate {

    @FXML
    private TextField animalidfield;
    @FXML
    private TextField clientidfield;

    @FXML
    private TextArea messagefield;

    @FXML
    private TextField phonefield;

    @FXML
    private TextField addressfield;

    @FXML
    private Button saveButton;

    @FXML
    private TextField statusField;

    private adoptionRequest requestSelected;
    private ListView<adoptionRequest> listView;

    public void setRequest(adoptionRequest request) {
        this.requestSelected = request;

        animalidfield.setText(String.valueOf(request.getAnimal_id()));
        clientidfield.setText(String.valueOf(request.getClient_id()));
        messagefield.setText(request.getMessage());
        phonefield.setText(request.getPhone());
        addressfield.setText(request.getAddress());
        statusField.setText(request.getStatus().toString());
    }

    public void setListView(ListView<adoptionRequest> listView) {
        this.listView = listView;
    }

    @FXML
    void handleSave(ActionEvent event) {
        try {
            requestSelected.setAnimal_id(Integer.parseInt(animalidfield.getText()));
            requestSelected.setClient_id(Integer.parseInt(clientidfield.getText()));
            requestSelected.setMessage(messagefield.getText());
            requestSelected.setPhone(phonefield.getText());
            requestSelected.setAddress(addressfield.getText());
            requestSelected.setStatus(com.esprit.entities.adoptionRequest.status.valueOf(statusField.getText()));

            //  Sauvegarder dans la BD
            adoptionservices service = new adoptionservices();
            service.modifier(requestSelected);

            if (listView != null) {
                listView.refresh();

            }

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRequest.fxml"));
            Parent root = loader.load();

            Stage listStage = new Stage();
            listStage.setScene(new Scene(root));
            // Maximize the a
            listStage.setMaximized(true);
            listStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}