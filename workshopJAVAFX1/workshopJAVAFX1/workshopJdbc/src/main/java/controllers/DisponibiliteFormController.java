package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import services.ServiceDisponibilite;
import utils.EditState;
import utils.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DisponibiliteFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField vetIdField;
    @FXML
    private ComboBox<Disponibilite.Statut> statutBox;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private Button saveButton;

    private final ServiceDisponibilite service = new ServiceDisponibilite();
    private Disponibilite editing;

    @FXML
    public void initialize() {
        statutBox.setItems(FXCollections.observableArrayList(Disponibilite.Statut.values()));

        editing = EditState.disponibiliteToEdit;
        if (editing != null) {
            titleLabel.setText("Modifier disponibilite #" + editing.getId_disponibilite());
            saveButton.setText("Mettre a jour");
            fillForm(editing);
        }
    }

    @FXML
    private void onSave() {
        try {
            int vetId = ValidationUtils.parsePositiveInt(vetIdField.getText(), "vet_id");

            Disponibilite.Statut statut = statutBox.getValue();
            if (statut == null) {
                throw new IllegalArgumentException("statut est obligatoire (valable/nonvalable).");
            }

            LocalTime start = ValidationUtils.parseHourMinute(startTimeField.getText(), "starttime");
            LocalTime end = ValidationUtils.parseHourMinute(endTimeField.getText(), "endtime");
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("endtime doit etre apres starttime.");
            }

            Disponibilite d = new Disponibilite(vetId, formatDbTime(start), formatDbTime(end), statut);
            if (editing == null) {
                service.add(d);
                showInfo("Disponibilite ajoutee.");
                clearForm();
            } else {
                d.setId_disponibilite(editing.getId_disponibilite());
                service.update(d);
                EditState.disponibiliteToEdit = null;
                showInfo("Disponibilite mise a jour.");
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onGoList(javafx.event.ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        utils.ViewNavigator.goTo(event, "/DisponibiliteList.fxml", "Disponibilite - Affichage");
    }

    @FXML
    private void onGoHome(javafx.event.ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        utils.ViewNavigator.goTo(event, "/Home.fxml", "Gestion Veterinaire");
    }

    private void fillForm(Disponibilite d) {
        vetIdField.setText(String.valueOf(d.getId()));
        statutBox.setValue(d.getStatut());
        startTimeField.setText(normalizeTimeForInput(d.getStarttime()));
        endTimeField.setText(normalizeTimeForInput(d.getEndtime()));
    }

    private String normalizeTimeForInput(String dbValue) {
        if (dbValue == null) {
            return "";
        }
        String trimmed = dbValue.trim();
        if (trimmed.length() >= 5) {
            return trimmed.substring(0, 5);
        }
        return trimmed;
    }

    private String formatDbTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void clearForm() {
        vetIdField.clear();
        statutBox.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
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
