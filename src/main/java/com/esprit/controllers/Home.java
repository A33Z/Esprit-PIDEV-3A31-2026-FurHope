package com.esprit.controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Home {


        @FXML
        private void goLoginClient(ActionEvent event) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Connexion Client");
                stage.setMaximized(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
