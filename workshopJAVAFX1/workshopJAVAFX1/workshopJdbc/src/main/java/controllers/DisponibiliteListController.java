package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import services.ServiceDisponibilite;
import utils.EditState;
import utils.ViewNavigator;

import java.sql.SQLException;

public class DisponibiliteListController {

    @FXML
    private ListView<Disponibilite> disponibiliteList;
    @FXML
    private Label detailsLabel;

    private final ServiceDisponibilite service = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        disponibiliteList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("#" + item.getId_disponibilite() + "  | Vet " + item.getId() + " | "
                            + item.getStarttime() + " -> " + item.getEndtime() + " | " + item.getStatut().name().toLowerCase());
                }
            }
        });

        disponibiliteList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                detailsLabel.setText("Selectionnez une disponibilite pour voir details, modifier ou supprimer.");
            } else {
                detailsLabel.setText("Selection: id_disponibilite=" + selected.getId_disponibilite()
                        + ", vet_id=" + selected.getId()
                        + ", statut=" + selected.getStatut().name().toLowerCase()
                        + ", starttime=" + selected.getStarttime()
                        + ", endtime=" + selected.getEndtime());
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
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Disponibilite - Formulaire");
    }

    @FXML
    private void onEdit(javafx.event.ActionEvent event) {
        Disponibilite selected = disponibiliteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez une disponibilite a modifier.");
            return;
        }
        EditState.disponibiliteToEdit = selected;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Disponibilite - Modification");
    }

    @FXML
    private void onDelete() {
        Disponibilite selected = disponibiliteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez une disponibilite a supprimer.");
            return;
        }

        try {
            service.delete(selected.getId_disponibilite());
            showInfo("Disponibilite supprimee.");
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
            disponibiliteList.setItems(FXCollections.observableArrayList(service.read()));
            detailsLabel.setText("Selectionnez une disponibilite pour voir details, modifier ou supprimer.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Affichage disponibilites");
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
