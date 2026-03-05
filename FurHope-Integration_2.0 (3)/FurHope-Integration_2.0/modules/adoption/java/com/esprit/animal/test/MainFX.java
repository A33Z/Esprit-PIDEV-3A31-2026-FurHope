package com.esprit.animal.test;

import com.esprit.animal.i18n.LanguageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    private static final String HOME_VIEW = "/animal/Home.fxml";
    private static Stage primaryStage;
    private static String currentView = HOME_VIEW;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        loadScene(HOME_VIEW);
    }

    public static void loadScene(String fxmlPath) {
        try {
            currentView = fxmlPath;
            FXMLLoader loader = new FXMLLoader(
                    MainFX.class.getResource(fxmlPath),
                    LanguageManager.getBundle()
            );
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            primaryStage.setTitle(LanguageManager.get("title"));
            primaryStage.setMaximized(true);
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

    public static void main(String[] args) {
        launch(args);
    }

}

