package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Rendezvous;
import services.ServiceRendezvous;
import utils.EditState;
import utils.ViewNavigator;

import java.sql.SQLException;

public class RendezvousListController {

    @FXML
    private ListView<Rendezvous> rendezvousList;
    @FXML
    private Label detailsLabel;

    private final ServiceRendezvous service = new ServiceRendezvous();

    @FXML
    public void initialize() {
        rendezvousList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Rendezvous item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("#" + item.getId_rdv() + " | status=" + item.getStatus() + " | client=" + item.getClient_id()
                            + " | vet=" + item.getVet_id() + " | animal=" + item.getAnimal_id());
                }
            }
        });

        rendezvousList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                detailsLabel.setText("Selectionnez un rendez-vous pour modifier/supprimer.");
            } else {
                detailsLabel.setText("Selection: id_rdv=" + selected.getId_rdv()
                        + ", status=" + selected.getStatus()
                        + ", description=" + (selected.getDescription() == null ? "" : selected.getDescription())
                        + ", client_id=" + selected.getClient_id()
                        + ", vet_id=" + selected.getVet_id()
                        + ", animal_id=" + selected.getAnimal_id()
                        + ", disponibilite_id=" + selected.getDisponibilite_id()
                        + ", app_date=" + selected.getApp_date()
                        + ", app_time=" + selected.getApp_time());
            }
        });

        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd(javafx.event.ActionEvent event) {
        EditState.rendezvousToEdit = null;
        ViewNavigator.goTo(event, "/RendezvousForm.fxml", "Rendez-vous - Formulaire");
    }

    @FXML
    private void onEdit(javafx.event.ActionEvent event) {
        Rendezvous selected = rendezvousList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez un rendez-vous a modifier.");
            return;
        }
        EditState.rendezvousToEdit = selected;
        ViewNavigator.goTo(event, "/RendezvousForm.fxml", "Rendez-vous - Modification");
    }

    @FXML
    private void onDelete() {
        Rendezvous selected = rendezvousList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez un rendez-vous a supprimer.");
            return;
        }

        try {
            service.delete(selected.getId_rdv());
            showInfo("Rendez-vous supprime.");
            refresh();
        } catch (SQLException e) {
            showError("Suppression impossible: " + e.getMessage());
        }
    }

    @FXML
    private void onBackHome(javafx.event.ActionEvent event) {
        ViewNavigator.goTo(event, "/Home.fxml", "Gestion Veterinaire");
    }

    private void refresh() {
        try {
            rendezvousList.setItems(FXCollections.observableArrayList(service.read()));
            detailsLabel.setText("Selectionnez un rendez-vous pour modifier/supprimer.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Affichage rendez-vous");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
