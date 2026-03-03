package controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import model.Rendezvous;
import services.ServiceDisponibilite;
import services.ServiceRendezvous;
import utils.ValidationUtils;

import java.sql.SQLException;
import java.util.List;

public class RendezvousController {

    @FXML private TableView<Rendezvous> rendezvousTable;
    @FXML private TableColumn<Rendezvous, Integer> colRdvId;
    @FXML private TableColumn<Rendezvous, String>  colStatus;
    @FXML private TableColumn<Rendezvous, Integer> colClient;
    @FXML private TableColumn<Rendezvous, Integer> colVet;
    @FXML private TableColumn<Rendezvous, Integer> colAnimal;
    @FXML private TableColumn<Rendezvous, Integer> colDisponibilite;
    @FXML private TableColumn<Rendezvous, String>  colSlotStart; // ✅ corrigé

    @FXML private ComboBox<String>       statusBox;
    @FXML private TextField              clientIdField;
    @FXML private TextField              animalIdField;
    @FXML private TextArea               descriptionArea;
    @FXML private ComboBox<Disponibilite> disponibiliteBox;
    @FXML private TextField              vetIdField;

    private final ServiceRendezvous    serviceRendezvous    = new ServiceRendezvous();
    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        statusBox.setItems(FXCollections.observableArrayList(
                "EN_ATTENTE", "CONFIRME", "ANNULE"
        ));

        colRdvId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId_rdv()).asObject());
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        colClient.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getClient_id()).asObject());
        colVet.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getVet_id()).asObject());
        colAnimal.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getAnimal_id()).asObject());
        colDisponibilite.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getDisponibilite_id()).asObject());


        colSlotStart.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getSlotStart()));

        disponibiliteBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDisponibilite(item));
            }
        });
        disponibiliteBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDisponibilite(item));
            }
        });

        disponibiliteBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, selected) -> {
                    if (selected != null)
                        vetIdField.setText(String.valueOf(selected.getId()));
                }
        );

        rendezvousTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, selected) -> {
                    if (selected != null) fillForm(selected);
                }
        );

        refreshDisponibilites();
        refreshTable();
    }

    @FXML
    private void onCreate() {
        try {
            Rendezvous rdv = readFromForm();
            serviceRendezvous.add(rdv);
            showInfo("Rendez-vous ajouté avec succès.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        Rendezvous selected = rendezvousTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un rendez-vous à modifier.");
            return;
        }
        try {
            Rendezvous rdv = readFromForm();
            rdv.setId_rdv(selected.getId_rdv());
            serviceRendezvous.update(rdv);
            showInfo("Rendez-vous modifié avec succès.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
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
            showInfo("Rendez-vous supprimé avec succès.");
            clearForm();
            refreshTable();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onClear() { clearForm(); }

    @FXML
    private void onRefreshDisponibilites() { refreshDisponibilites(); }


    private Rendezvous readFromForm() {
        String status = statusBox.getValue();
        if (status == null || status.isBlank())
            throw new IllegalArgumentException("Status est obligatoire.");

        int clientId = ValidationUtils.parsePositiveInt(clientIdField.getText(), "Client ID");
        int animalId = ValidationUtils.parsePositiveInt(animalIdField.getText(), "Animal ID");
        String description = ValidationUtils.requireMinLength(
                descriptionArea.getText(), "Description", 5);

        Disponibilite selectedDispo = disponibiliteBox.getValue();
        if (selectedDispo == null)
            throw new IllegalArgumentException("Disponibilité est obligatoire.");
        if (selectedDispo.getStatut() != Disponibilite.Statut.VALABLE)
            throw new IllegalArgumentException("Disponibilité sélectionnée n'est pas VALABLE.");

        Rendezvous rdv = new Rendezvous();
        rdv.setStatus(status);
        rdv.setDescription(description);
        rdv.setClient_id(clientId);
        rdv.setVet_id(selectedDispo.getId());
        rdv.setAnimal_id(animalId);
        rdv.setDisponibilite_id(selectedDispo.getId_disponibilite());
        rdv.setSlotStart(null);
        return rdv;
    }

    private void fillForm(Rendezvous rdv) {
        statusBox.setValue(rdv.getStatus());
        clientIdField.setText(String.valueOf(rdv.getClient_id()));
        animalIdField.setText(String.valueOf(rdv.getAnimal_id()));
        descriptionArea.setText(rdv.getDescription());
        vetIdField.setText(String.valueOf(rdv.getVet_id()));

        for (Disponibilite d : disponibiliteBox.getItems()) {
            if (d.getId_disponibilite() == rdv.getDisponibilite_id()) {
                disponibiliteBox.setValue(d);
                break;
            }
        }
    }

    private void clearForm() {
        rendezvousTable.getSelectionModel().clearSelection();
        statusBox.setValue(null);
        clientIdField.clear();
        animalIdField.clear();
        descriptionArea.clear();
        disponibiliteBox.setValue(null);
        vetIdField.clear();
    }

    private void refreshTable() {
        try {
            rendezvousTable.setItems(
                    FXCollections.observableArrayList(serviceRendezvous.read()));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void refreshDisponibilites() {
        try {
            List<Disponibilite> dispos = serviceDisponibilite.readValables();
            disponibiliteBox.setItems(FXCollections.observableArrayList(dispos));
        } catch (SQLException e) {
            showError("Chargement disponibilités impossible: " + e.getMessage());
        }
    }

    private String formatDisponibilite(Disponibilite d) {
        return "#" + d.getId_disponibilite() + " | Vet " + d.getId() +
                " | " + d.getStarttime() + " → " + d.getEndtime();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText("Contrôle de saisie");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}