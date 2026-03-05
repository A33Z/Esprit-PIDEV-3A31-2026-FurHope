package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import utils.EditState;
import utils.SessionManager;
import utils.ViewNavigator;

public class VetDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(() -> {
            welcomeLabel.setText("Bonjour Dr. " + SessionManager.getUserNom() + "!");
        });
    }

    @FXML
    private void goDisponibilites(ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Mes Disponibilites");
    }

    @FXML
    private void goRendezvous(ActionEvent event) {
        try {
            ViewNavigator.goTo(event, "/RendezvousList.fxml", "Gerer Rendez-vous");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur Navigation");
            alert.setContentText("Erreur: " + e.getMessage()
                    + "\nCause: " + (e.getCause() != null ? e.getCause().getMessage() : "inconnue"));
            alert.showAndWait();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionContext.clear();
        SessionManager.logout();
        ViewNavigator.goTo(event, "/Welcome.fxml", "FurHope");
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/accueil.fxml", "FurHope");
    }

    @FXML
    private void goStats(ActionEvent event) {
        ViewNavigator.goTo(event, "/StatsDashboard.fxml", "Mes Statistiques");
    }
}

