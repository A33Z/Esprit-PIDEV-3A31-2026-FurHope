package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import model.Rendezvous;
import services.ServiceDisponibilite;
import services.ServiceRendezvous;
import utils.EditState;
import utils.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RendezvousFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField statusField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField clientIdField;
    @FXML
    private ComboBox<Disponibilite> disponibiliteBox;
    @FXML
    private TextField vetIdField;
    @FXML
    private TextField animalIdField;
    @FXML
    private DatePicker appDatePicker;
    @FXML
    private TextField appTimeField;
    @FXML
    private Button saveButton;

    private final ServiceRendezvous serviceRdv = new ServiceRendezvous();
    private final ServiceDisponibilite serviceDispo = new ServiceDisponibilite();

    private Rendezvous editing;

    @FXML
    public void initialize() {
        disponibiliteBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDispo(item));
            }
        });
        disponibiliteBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDispo(item));
            }
        });

        disponibiliteBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                vetIdField.setText(String.valueOf(selected.getId()));
            } else {
                vetIdField.clear();
            }
        });

        loadDisponibilites();

        editing = EditState.rendezvousToEdit;
        if (editing != null) {
            titleLabel.setText("Modifier rendez-vous #" + editing.getId_rdv());
            saveButton.setText("Mettre a jour");
            fillForm(editing);
        }
    }

    @FXML
    private void onSave() {
        try {
            String status = ValidationUtils.requireMinLength(statusField.getText(), "status", 3);
            if (status.length() > 30) {
                throw new IllegalArgumentException("status doit contenir max 30 caracteres.");
            }

            String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

            int clientId = ValidationUtils.parsePositiveInt(clientIdField.getText(), "client_id");
            int animalId = ValidationUtils.parsePositiveInt(animalIdField.getText(), "animal_id");

            Disponibilite selectedDispo = disponibiliteBox.getValue();
            if (selectedDispo == null) {
                throw new IllegalArgumentException("disponibilite_id est obligatoire.");
            }
            int vetId = selectedDispo.getId();

            LocalDate appDate = ValidationUtils.requireDate(appDatePicker.getValue(), "app_date");
            LocalTime appTime = ValidationUtils.parseHourMinute(appTimeField.getText(), "app_time");

            Rendezvous rdv = new Rendezvous(
                    status,
                    description,
                    clientId,
                    vetId,
                    animalId,
                    selectedDispo.getId_disponibilite(),
                    appDate.toString(),
                    appTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            );

            if (editing == null) {
                serviceRdv.add(rdv);
                showInfo("Rendez-vous ajoute.");
                clearForm();
            } else {
                rdv.setId_rdv(editing.getId_rdv());
                serviceRdv.update(rdv);
                EditState.rendezvousToEdit = null;
                showInfo("Rendez-vous mis a jour.");
            }

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onGoList(javafx.event.ActionEvent event) {
        EditState.rendezvousToEdit = null;
        utils.ViewNavigator.goTo(event, "/RendezvousList.fxml", "Rendez-vous - Affichage");
    }

    @FXML
    private void onGoHome(javafx.event.ActionEvent event) {
        EditState.rendezvousToEdit = null;
        utils.ViewNavigator.goTo(event, "/Home.fxml", "Gestion Veterinaire");
    }

    @FXML
    private void onRefreshDisponibilites() {
        loadDisponibilites();
    }

    private void fillForm(Rendezvous rdv) {
        statusField.setText(rdv.getStatus());
        descriptionArea.setText(rdv.getDescription());
        clientIdField.setText(String.valueOf(rdv.getClient_id()));
        animalIdField.setText(String.valueOf(rdv.getAnimal_id()));
        appTimeField.setText(rdv.getApp_time());

        try {
            appDatePicker.setValue(LocalDate.parse(rdv.getApp_date()));
        } catch (Exception e) {
            appDatePicker.setValue(null);
        }

        Disponibilite matched = null;
        for (Disponibilite d : disponibiliteBox.getItems()) {
            if (d.getId_disponibilite() == rdv.getDisponibilite_id()) {
                matched = d;
                break;
            }
        }
        disponibiliteBox.setValue(matched);
        vetIdField.setText(String.valueOf(rdv.getVet_id()));
    }

    private void clearForm() {
        statusField.clear();
        descriptionArea.clear();
        clientIdField.clear();
        animalIdField.clear();
        disponibiliteBox.setValue(null);
        vetIdField.clear();
        appDatePicker.setValue(null);
        appTimeField.clear();
    }

    private void loadDisponibilites() {
        try {
            disponibiliteBox.setItems(FXCollections.observableArrayList(serviceDispo.read()));
        } catch (SQLException e) {
            showError("Chargement disponibilites impossible: " + e.getMessage());
        }
    }

    private String formatDispo(Disponibilite d) {
        return "#" + d.getId_disponibilite() + " | vet=" + d.getId() + " | " + d.getStarttime() + "-" + d.getEndtime()
                + " | " + d.getStatut().name().toLowerCase();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur saisie");
        alert.setHeaderText("Controle de saisie");
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
