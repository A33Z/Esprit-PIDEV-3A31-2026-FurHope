package com.esprit.animal.test;

import com.esprit.animal.i18n.LanguageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static final String DEFAULT_VIEW = "/animal/views/animal-view.fxml";
    private static Stage primaryStage;
    private static String currentView = DEFAULT_VIEW;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        loadScene(DEFAULT_VIEW);
    }

    public static void loadScene(String fxmlPath) {
        try {
            currentView = fxmlPath;
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath),
                    LanguageManager.getBundle()
            );
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1000, 700);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            primaryStage.setTitle(LanguageManager.get("title"));
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }

    public static void switchLanguage(String languageCode) {
        LanguageManager.setLanguage(languageCode);
        reloadCurrentScene();
    }

    public static void reloadCurrentScene() {
        loadScene(currentView);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

