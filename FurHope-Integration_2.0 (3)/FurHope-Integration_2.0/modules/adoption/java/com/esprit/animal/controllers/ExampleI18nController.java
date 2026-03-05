package com.esprit.animal.controllers;

import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.test.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class ExampleI18nController {

    @FXML
    private void switchToFrench(ActionEvent event) {
        MainApp.switchLanguage("fr");
    }

    @FXML
    private void switchToEnglish(ActionEvent event) {
        MainApp.switchLanguage("en");
    }

    @FXML
    private void addAnimal(ActionEvent event) {
        showInfo(LanguageManager.get("addAnimal"));
    }

    @FXML
    private void deleteAnimal(ActionEvent event) {
        showInfo(LanguageManager.get("delete"));
    }

    @FXML
    private void updateAnimal(ActionEvent event) {
        showInfo(LanguageManager.get("update"));
    }

    @FXML
    private void save(ActionEvent event) {
        showInfo(LanguageManager.get("save"));
    }

    @FXML
    private void cancel(ActionEvent event) {
        showInfo(LanguageManager.get("cancel"));
    }

    private void showInfo(String action) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(LanguageManager.get("title"));
        alert.setHeaderText(null);
        alert.setContentText(action);
        alert.showAndWait();
    }
}

