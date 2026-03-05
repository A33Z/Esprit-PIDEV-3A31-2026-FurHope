package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.ViewNavigator;

public class HomeController {

    // 🔐 Espace Vétérinaire
    @FXML
    private void goLogin(ActionEvent event) {
        ViewNavigator.goTo(event, "/Login.fxml", "Login Vétérinaire");
    }

    // 👤 Espace Client
    @FXML
    private void goLoginClient(ActionEvent event) {
        ViewNavigator.goTo(event, "/LoginClient.fxml", "Espace Client");
    }
}