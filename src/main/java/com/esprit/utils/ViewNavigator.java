package com.esprit.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewNavigator {

        private ViewNavigator() {
        }

        public static void goTo(ActionEvent event, String fxmlPath) {
            try {
                FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root, 1200, 760));
                stage.show();
            } catch (IOException e) {
                throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
            }
        }
    }


