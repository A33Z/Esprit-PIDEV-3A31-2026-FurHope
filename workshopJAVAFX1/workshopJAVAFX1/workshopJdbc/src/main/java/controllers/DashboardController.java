package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import utils.EditState;
import utils.SessionManager;
import utils.ViewNavigator;

public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        // ✅ Rediriger vers StatsDashboard
        javafx.application.Platform.runLater(() -> {
            welcomeLabel.setText("Bonjour Dr. " + SessionManager.getUserNom() + " 👋");
        });
    }

    @FXML
    private void goDisponibilites(ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Mes Disponibilités");
    }

    @FXML
    private void goRendezvous(ActionEvent event) {
        // ✅ Debug — voir l'erreur exacte
        try {
            ViewNavigator.goTo(event, "/RendezvousList.fxml", "Gérer Rendez-vous");
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Erreur Navigation");
            alert.setContentText("Erreur: " + e.getMessage() +
                    "\nCause: " + (e.getCause() != null ? e.getCause().getMessage() : "inconnue"));
            alert.showAndWait();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionManager.logout();
        ViewNavigator.goTo(event, "/Home.fxml", "Clinique Vétérinaire");
    }
    @FXML
    private void goStats(ActionEvent event) {
        ViewNavigator.goTo(event, "/StatsDashboard.fxml", "Mes Statistiques");
    }
}