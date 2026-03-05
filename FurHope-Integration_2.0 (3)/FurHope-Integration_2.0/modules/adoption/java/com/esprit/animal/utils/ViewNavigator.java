package com.esprit.animal.utils;

import com.esprit.animal.i18n.LanguageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewNavigator {

        private ViewNavigator() {
        }

        public static void goTo(ActionEvent event, String fxmlPath) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        ViewNavigator.class.getResource(fxmlPath),
                        LanguageManager.getBundle()
                );
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                StageSceneHelper.setScene(stage, root);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
            }
        }
    }


