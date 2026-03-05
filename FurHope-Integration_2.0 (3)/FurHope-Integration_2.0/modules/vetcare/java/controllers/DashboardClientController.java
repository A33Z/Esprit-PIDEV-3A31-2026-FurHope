package controllers;

import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import utils.SessionManager;
import utils.ViewNavigator;

public class DashboardClientController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bonjour " + SessionManager.getUserNom() + "!");
    }

    @FXML
    private void goListeVets(ActionEvent event) {
        ViewNavigator.goTo(event, "/ListeVeterinaires.fxml", "Nos Veterinaires");
    }

    @FXML
    private void goMesRdv(ActionEvent event) {
        ViewNavigator.goTo(event, "/MesRendezvous.fxml", "Mes Rendez-vous");
    }

    @FXML
    private void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatbotView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root, 980, 720);
            ThemeManager.applyToScene(scene);

            stage.setTitle("Assistant Medical IA");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionContext.clear();
        SessionManager.logout();
        ViewNavigator.goTo(event, "/Welcome.fxml", "FurHope");
    }

    @FXML
    private void goMesAvis(ActionEvent event) {
        ViewNavigator.goTo(event, "/MesAvis.fxml", "Mes Avis");
    }

    @FXML
    private void goDashboard(ActionEvent event) {
        ViewNavigator.goTo(event, "/dashboard.fxml", "Dashboard");
    }
}

