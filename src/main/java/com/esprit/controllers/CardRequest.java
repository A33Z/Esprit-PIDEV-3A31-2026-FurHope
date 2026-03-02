package com.esprit.controllers;

import com.esprit.Services.animalServices;
import com.esprit.entities.adoptionRequest;
import com.esprit.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CardRequest {
    @FXML
    private VBox cardRoot;

    @FXML
    private Label statusLabel;

    private adoptionRequest currentRequest;

    public void setData(adoptionRequest request) {
        this.currentRequest = request;
        statusLabel.setText(request.getStatus().toString());
        // Clic sur la card → ouvrir détails
        cardRoot.setOnMouseClicked(event -> openDetail());
    }

    // Ouvre la fenêtre de détail de la demande
    private void openDetail() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RequestDetailAdmin.fxml"));
            AnchorPane root = loader.load(); // root réel du FXML

            RequestDetailAdmin controller = loader.getController();
            controller.setRequest(currentRequest);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Request Details");
            stage.setMaximized(true);

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}