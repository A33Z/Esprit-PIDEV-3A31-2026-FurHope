package utils;

import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class ViewNavigator {

    private ViewNavigator() {
    }

    public static void goTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 760);
            ThemeManager.applyToScene(scene);
            stage.setScene(scene);
            stage.setTitle(title);
            ThemeManager.applyToStage(stage);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
        }
    }
}
