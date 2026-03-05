package com.esprit.animal.utils;

import com.esprit.utils.ThemeManager;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class StageSceneHelper {

    private StageSceneHelper() {
    }

    public static void setScene(Stage stage, Parent root) {
        if (stage == null || root == null) {
            return;
        }

        Scene scene = new Scene(root);
        ThemeManager.applyToScene(scene);
        stage.setScene(scene);
        ThemeManager.applyToStage(stage);
    }
}
