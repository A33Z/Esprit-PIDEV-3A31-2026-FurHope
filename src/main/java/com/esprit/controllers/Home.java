package com.esprit.controllers;

import com.esprit.i18n.LanguageManager;
import com.esprit.test.MainFX;
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
                        getClass().getResource("/login.fxml"),
                        LanguageManager.getBundle()
                );
                Parent root = loader.load();
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
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
