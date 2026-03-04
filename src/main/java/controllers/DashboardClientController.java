package controllers;

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

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bonjour " + SessionManager.getUserNom() + " ðŸ‘‹");
    }

    @FXML
    private void goListeVets(ActionEvent event) {
        ViewNavigator.goTo(event, "/ListeVeterinaires.fxml", "Nos VÃ©tÃ©rinaires");
    }

    // âœ… NOUVEAU
    @FXML
    private void goMesRdv(ActionEvent event) {
        ViewNavigator.goTo(event, "/MesRendezvous.fxml", "Mes Rendez-vous");
    }

    @FXML
    private void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ChatbotView.fxml")
            );
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("ðŸ©º Assistant MÃ©dical IA");
            stage.setScene(new Scene(root, 620, 550));
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
