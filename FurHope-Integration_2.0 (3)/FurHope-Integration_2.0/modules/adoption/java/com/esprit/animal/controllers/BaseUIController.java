package com.esprit.animal.controllers;

import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.utils.StageSceneHelper;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class BaseUIController {

    protected String getViewPath() {
        return null;
    }

    protected String getBackViewPath() {
        return "/animal/AfficherAnimal.fxml";
    }

    protected void onControllerReloaded(Object controller) {
    }

    @FXML
    protected void setFrench(ActionEvent event) {
        switchLanguage(event, "fr");
    }

    @FXML
    protected void setEnglish(ActionEvent event) {
        switchLanguage(event, "en");
    }

    @FXML
    protected void goHome(ActionEvent event) {
        navigate(event, "/accueil.fxml");
    }

    @FXML
    protected void goBack(ActionEvent event) {
        navigate(event, getBackViewPath());
    }

    protected void navigate(Event event, String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }

        try {
            FXMLLoader loader = fxmlPath.startsWith("/animal/")
                    ? new FXMLLoader(getClass().getResource(fxmlPath), LanguageManager.getBundle())
                    : new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = resolveStage(event);
            if (stage == null) {
                return;
            }
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed for: " + fxmlPath, e);
        }
    }

    protected FXMLLoader createLoader(String fxmlPath) {
        return new FXMLLoader(getClass().getResource(fxmlPath), LanguageManager.getBundle());
    }

    protected Parent loadView(String fxmlPath) throws IOException {
        return createLoader(fxmlPath).load();
    }

    private void switchLanguage(ActionEvent event, String languageCode) {
        String viewPath = getViewPath();
        if (viewPath == null || viewPath.isBlank()) {
            return;
        }

        try {
            LanguageManager.setLanguage(languageCode);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath), LanguageManager.getBundle());
            Parent root = loader.load();
            Object reloadedController = loader.getController();
            onControllerReloaded(reloadedController);

            Stage stage = resolveStage(event);
            if (stage == null) {
                return;
            }

            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Language switch failed for: " + viewPath, e);
        }
    }

    protected Stage resolveStage(Event event) {
        if (event == null) {
            return null;
        }
        Object source = event.getSource();
        if (!(source instanceof Node)) {
            return null;
        }
        return (Stage) ((Node) source).getScene().getWindow();
    }
}
