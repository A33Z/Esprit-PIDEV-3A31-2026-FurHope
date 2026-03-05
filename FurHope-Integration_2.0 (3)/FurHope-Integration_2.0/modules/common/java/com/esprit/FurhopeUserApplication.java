package com.esprit;

import com.esprit.furhope.services.DbHealthCheckService;
import com.esprit.utils.ThemeManager;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class FurhopeUserApplication {

    private static Stage primaryStage;

    private FurhopeUserApplication() {
    }

    public static void loadRoleSelection() {
        if (primaryStage == null) {
            return;
        }
        try {
            Parent root = FXMLLoader.load(
                    FurhopeUserApplication.class.getResource("/RoleSelection.fxml")
            );

            Scene scene = new Scene(root, 1100, 700);
            ThemeManager.applyToScene(scene);

            primaryStage.setTitle("FurHope - Role Selection");
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Application.launch(FxRuntime.class, args);
    }

    public static class FxRuntime extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            primaryStage = stage;
            if (!DbHealthCheckService.verifyConnectionAtStartup()) {
                stage.close();
                Platform.exit();
                return;
            }
            ThemeManager.bindStage(stage);

            Parent root = FXMLLoader.load(
                    FxRuntime.class.getResource("/Welcome.fxml")
            );

            Scene scene = new Scene(root, 1100, 700);
            ThemeManager.applyToScene(scene);

            stage.setTitle("FurHope - Animal Shelter Platform");
            stage.setScene(scene);
            ThemeManager.applyToStage(stage);
            stage.show();
        }
    }
}
