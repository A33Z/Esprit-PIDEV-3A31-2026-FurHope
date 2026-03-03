package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import utils.EditState;
import utils.SessionManager;
import utils.ViewNavigator;

public class VetDashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        // âœ… Rediriger vers StatsDashboard
        javafx.application.Platform.runLater(() -> {
            welcomeLabel.setText("Bonjour Dr. " + SessionManager.getUserNom() + " ðŸ‘‹");
        });
    }

    @FXML
    private void goDisponibilites(ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Mes DisponibilitÃ©s");
    }

    @FXML
    private void goRendezvous(ActionEvent event) {
        // âœ… Debug â€” voir l'erreur exacte
        try {
            ViewNavigator.goTo(event, "/RendezvousList.fxml", "GÃ©rer Rendez-vous");
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

