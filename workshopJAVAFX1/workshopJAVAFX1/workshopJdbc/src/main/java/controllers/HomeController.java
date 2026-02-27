package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.EditState;
import utils.ViewNavigator;
import java.io.IOException;

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

    @FXML
    private void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ChatbotView.fxml")  // ← sans "views/"
            );
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🩺 Assistant Médical IA");
            stage.setScene(new Scene(root, 620, 550));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace(); // ← affiche l'erreur dans la console
            System.out.println("ERREUR: " + e.getMessage());
        }
    }}