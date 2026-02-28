package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class HomeGuestController {

    public void goToSignIn(ActionEvent event) {
        switchScene(event, "/signin.fxml");
    }

    public void goToSignUp(ActionEvent event) {
        switchScene(event, "/signup.fxml");
    }

    public void goBack(ActionEvent event) {
        switchScene(event, "/accueil.fxml");
    }

    public void openProtectedFeature() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Required");
        alert.setContentText("This feature opens after login. Your teammate can connect the final page later.");
        alert.show();
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

