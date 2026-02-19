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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RendezvousController {

    @FXML
    private TableView<Rendezvous> rendezvousTable;
    @FXML
    private TableColumn<Rendezvous, Integer> colRdvId;
    @FXML
    private TableColumn<Rendezvous, String> colStatus;
    @FXML
    private TableColumn<Rendezvous, Integer> colClient;
    @FXML
    private TableColumn<Rendezvous, Integer> colVet;
    @FXML
    private TableColumn<Rendezvous, Integer> colAnimal;
    @FXML
    private TableColumn<Rendezvous, Integer> colDisponibilite;
    @FXML
    private TableColumn<Rendezvous, String> colDate;
    @FXML
    private TableColumn<Rendezvous, String> colTime;

    @FXML
    private ComboBox<String> statusBox;
    @FXML
    private TextField clientIdField;
    @FXML
    private TextField animalIdField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private DatePicker appDateField;
    @FXML
    private TextField appTimeField;
    @FXML
    private ComboBox<Disponibilite> disponibiliteBox;
    @FXML
    private TextField vetIdField;

    private final ServiceRendezvous serviceRendezvous = new ServiceRendezvous();
    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statusBox.setItems(FXCollections.observableArrayList("EN_ATTENTE", "CONFIRME", "ANNULE"));

        colRdvId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId_rdv()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colClient.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getClient_id()).asObject());
        colVet.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getVet_id()).asObject());
        colAnimal.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAnimal_id()).asObject());
        colDisponibilite.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDisponibilite_id()).asObject());
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getApp_date()));
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getApp_time()));

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

        disponibiliteBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                vetIdField.setText(String.valueOf(selected.getId()));
            }
        });

        rendezvousTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });

        refreshDisponibilites();
        refreshTable();
    }

    @FXML
    private void onCreate() {
        try {
            Rendezvous rendezvous = readFromForm();
            serviceRendezvous.add(rendezvous);
            showInfo("Rendez-vous ajoute avec succes.");
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
            showError("Selectionnez un rendez-vous a modifier.");
            return;
        }

        try {
            Rendezvous rendezvous = readFromForm();
            rendezvous.setId_rdv(selected.getId_rdv());
            serviceRendezvous.update(rendezvous);
            showInfo("Rendez-vous modifie avec succes.");
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
            showError("Selectionnez un rendez-vous a supprimer.");
            return;
        }

        try {
            serviceRendezvous.delete(selected.getId_rdv());
            showInfo("Rendez-vous supprime avec succes.");
            clearForm();
            refreshTable();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    @FXML
    private void onRefreshDisponibilites() {
        refreshDisponibilites();
    }

    private Rendezvous readFromForm() {
        String status = statusBox.getValue();
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status est obligatoire.");
        }

        int clientId = ValidationUtils.parsePositiveInt(clientIdField.getText(), "Client ID");
        int animalId = ValidationUtils.parsePositiveInt(animalIdField.getText(), "Animal ID");
        String description = ValidationUtils.requireMinLength(descriptionArea.getText(), "Description", 5);
        LocalDate appDate = ValidationUtils.requireDate(appDateField.getValue(), "Date rendez-vous");
        LocalTime appTime = ValidationUtils.parseHourMinute(appTimeField.getText(), "Heure rendez-vous");

        Disponibilite selectedDispo = disponibiliteBox.getValue();
        if (selectedDispo == null) {
            throw new IllegalArgumentException("Disponibilite est obligatoire.");
        }
        if (selectedDispo.getStatut() != Disponibilite.Statut.VALABLE) {
            throw new IllegalArgumentException("Disponibilite selectionnee n'est pas VALABLE.");
        }

        LocalDateTime appDateTime = LocalDateTime.of(appDate, appTime);
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(selectedDispo.getStarttime(), DATETIME_FORMAT);
            end = LocalDateTime.parse(selectedDispo.getEndtime(), DATETIME_FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format start/end invalide dans la disponibilite.");
        }

        if (appDateTime.isBefore(start) || appDateTime.isAfter(end)) {
            throw new IllegalArgumentException("Le rendez-vous doit etre dans l'intervalle de disponibilite du veterinaire.");
        }

        return new Rendezvous(
                status,
                description,
                clientId,
                selectedDispo.getId(),
                animalId,
                selectedDispo.getId_disponibilite(),
                appDate.toString(),
                appTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    private void fillForm(Rendezvous rendezvous) {
        statusBox.setValue(rendezvous.getStatus());
        clientIdField.setText(String.valueOf(rendezvous.getClient_id()));
        animalIdField.setText(String.valueOf(rendezvous.getAnimal_id()));
        descriptionArea.setText(rendezvous.getDescription());

        try {
            appDateField.setValue(LocalDate.parse(rendezvous.getApp_date()));
        } catch (Exception e) {
            appDateField.setValue(null);
        }
        appTimeField.setText(rendezvous.getApp_time());

        vetIdField.setText(String.valueOf(rendezvous.getVet_id()));

        Disponibilite matching = null;
        for (Disponibilite d : disponibiliteBox.getItems()) {
            if (d.getId_disponibilite() == rendezvous.getDisponibilite_id()) {
                matching = d;
                break;
            }
        }
        disponibiliteBox.setValue(matching);
    }

    private void clearForm() {
        rendezvousTable.getSelectionModel().clearSelection();
        statusBox.setValue(null);
        clientIdField.clear();
        animalIdField.clear();
        descriptionArea.clear();
        appDateField.setValue(null);
        appTimeField.clear();
        disponibiliteBox.setValue(null);
        vetIdField.clear();
    }

    private void refreshTable() {
        try {
            rendezvousTable.setItems(FXCollections.observableArrayList(serviceRendezvous.read()));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void refreshDisponibilites() {
        try {
            List<Disponibilite> disponibilites = serviceDisponibilite.readValables();
            disponibiliteBox.setItems(FXCollections.observableArrayList(disponibilites));
        } catch (SQLException e) {
            showError("Chargement disponibilites impossible: " + e.getMessage());
        }
    }

    private String formatDisponibilite(Disponibilite d) {
        return "#" + d.getId_disponibilite() + " | Vet " + d.getId() + " | " + d.getStarttime() + " -> " + d.getEndtime();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Controle de saisie / Operation");
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
