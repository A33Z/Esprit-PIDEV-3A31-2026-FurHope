package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import model.Rendezvous;
import services.ServiceRendezvous;
import utils.EditState;
import utils.ViewNavigator;

import java.sql.SQLException;
import java.util.List;

public class RendezvousListController {

    @FXML
    private TableView<Rendezvous> rendezvousTable;
    @FXML
    private TableColumn<Rendezvous, Integer> colId;
    @FXML
    private TableColumn<Rendezvous, String> colStatus;
    @FXML
    private TableColumn<Rendezvous, Integer> colClientId;
    @FXML
    private TableColumn<Rendezvous, Integer> colVetId;
    @FXML
    private TableColumn<Rendezvous, Integer> colAnimalId;
    @FXML
    private TableColumn<Rendezvous, Integer> colDisponibiliteId;

    private final ServiceRendezvous serviceRendezvous = new ServiceRendezvous();

    @FXML
    public void initialize() {
        // Initialiser les colonnes du TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id_rdv"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        //colClientId.setCellValueFactory(new PropertyValueFactory<>("client_id"));
        //colVetId.setCellValueFactory(new PropertyValueFactory<>("vet_id"));
        colAnimalId.setCellValueFactory(new PropertyValueFactory<>("animal_id"));
        colDisponibiliteId.setCellValueFactory(new PropertyValueFactory<>("disponibilite_id"));

        // Afficher les détails sélectionnés
        rendezvousTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // Tu peux mettre à jour un label ou un panneau de détails ici si tu veux
        });

        refreshTable();
    }

    @FXML
    private void onAdd(ActionEvent event) {
        EditState.rendezvousToEdit = null;
        ViewNavigator.goTo(event, "/RendezvousForm.fxml", "Ajouter Rendez-vous");
    }

    @FXML
    private void onEdit(ActionEvent event) {
        Rendezvous selected = rendezvousTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un rendez-vous à modifier.");
            return;
        }
        EditState.rendezvousToEdit = selected;
        ViewNavigator.goTo(event, "/RendezvousForm.fxml", "Modifier Rendez-vous");
    }

    @FXML
    private void onDelete() {
        Rendezvous selected = rendezvousTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un rendez-vous à supprimer.");
            return;
        }

        try {
            serviceRendezvous.delete(selected.getId_rdv());
            showInfo("Rendez-vous supprimé avec succès !");
            refreshTable();
        } catch (SQLException e) {
            showError("Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refreshTable();
    }

    @FXML
    private void onBackHome(ActionEvent event) {
        EditState.rendezvousToEdit = null;
        ViewNavigator.goTo(event, "/Home.fxml", "Accueil");
    }

    private void refreshTable() {
        try {
            List<Rendezvous> rdvs = serviceRendezvous.read();
            rendezvousTable.setItems(FXCollections.observableArrayList(rdvs));
        } catch (SQLException e) {
            showError("Erreur lors du chargement des rendez-vous : " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Contrôle de saisie / Opération");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}