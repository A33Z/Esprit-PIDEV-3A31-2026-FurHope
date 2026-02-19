package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import utils.EditState;
import utils.ViewNavigator;

public class HomeController {

    @FXML
    private void goDisponibiliteForm(ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Disponibilite - Formulaire");
    }

    @FXML
    private void goDisponibiliteList(ActionEvent event) {
        ViewNavigator.goTo(event, "/DisponibiliteList.fxml", "Disponibilite - Affichage");
    }

    @FXML
    private void goRendezvousForm(ActionEvent event) {
        EditState.rendezvousToEdit = null;
        ViewNavigator.goTo(event, "/RendezvousForm.fxml", "Rendez-vous - Formulaire");
    }

    @FXML
    private void goRendezvousList(ActionEvent event) {
        ViewNavigator.goTo(event, "/RendezvousList.fxml", "Rendez-vous - Affichage");
    }
}
