package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.test.MainFX;
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
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/animal/login.fxml"),
                        LanguageManager.getBundle()
                );
                Parent root = loader.load();
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                StageSceneHelper.setScene(stage, root);
                stage.setTitle(LanguageManager.get("login.title"));
                stage.setMaximized(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @FXML
        private void setFrench() {
            changeLanguage("fr");
        }

        @FXML
        private void setEnglish() {
            changeLanguage("en");
        }

        private void changeLanguage(String languageCode) {
            MainFX.switchLanguage(languageCode);
        }
    }



