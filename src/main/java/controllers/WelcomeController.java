package controllers;

import controllers.SessionContext;
import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WelcomeController {

    @FXML
    private void goToSignIn(ActionEvent event) {
        switchScene(event, "/signin.fxml");
    }

    @FXML
    private void goToSignUp(ActionEvent event) {
        switchScene(event, "/signup.fxml");
    }

    @FXML
    private void continueAsGuest(ActionEvent event) {
        SessionContext.clear();
        switchScene(event, "/accueil.fxml");
    }

    @FXML
    private void toggleDarkMode(ActionEvent event) {
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.toggle(scene);
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

