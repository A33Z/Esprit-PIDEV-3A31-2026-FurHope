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
    private TextField vetNomField; // ✅ nouveau champ pour le nom du vétérinaire
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
            titleLabel.setText("Modifier disponibilité #" + editing.getId_disponibilite());
            saveButton.setText("Mettre à jour");
            fillForm(editing);
        }
    }

    @FXML
    private void onSave() {
        try {
            int vetId = ValidationUtils.parsePositiveInt(vetIdField.getText(), "vet_id");

            // ✅ Lecture du nom du vétérinaire
            String vetNom = vetNomField.getText();
            if (vetNom == null || vetNom.trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom du vétérinaire est obligatoire.");
            }

            Disponibilite.Statut statut = statutBox.getValue();
            if (statut == null) {
                throw new IllegalArgumentException("Le statut est obligatoire (valable/nonvalable).");
            }

            LocalTime start = ValidationUtils.parseHourMinute(startTimeField.getText(), "starttime");
            LocalTime end = ValidationUtils.parseHourMinute(endTimeField.getText(), "endtime");
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("endtime doit être après starttime.");
            }

            // ✅ Création de l’objet avec vetNom inclus
            Disponibilite d = new Disponibilite(vetId, vetNom, formatDbTime(start), formatDbTime(end), statut);

            if (editing == null) {
                service.add(d);
                showInfo("Disponibilité ajoutée.");
                clearForm();
            } else {
                d.setId_disponibilite(editing.getId_disponibilite());
                service.update(d);
                EditState.disponibiliteToEdit = null;
                showInfo("Disponibilité mise à jour.");
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
        utils.ViewNavigator.goTo(event, "/Home.fxml", "Gestion Vétérinaire");
    }

    private void fillForm(Disponibilite d) {
        vetIdField.setText(String.valueOf(d.getId()));
        vetNomField.setText(d.getVetNom()); // ✅ remplir le champ vetNom
        statutBox.setValue(d.getStatut());
        startTimeField.setText(normalizeTimeForInput(d.getStarttime()));
        endTimeField.setText(normalizeTimeForInput(d.getEndtime()));
    }

    private String normalizeTimeForInput(String dbValue) {
        if (dbValue == null) return "";
        String trimmed = dbValue.trim();
        return trimmed.length() >= 5 ? trimmed.substring(0, 5) : trimmed;
    }

    private String formatDbTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void clearForm() {
        vetIdField.clear();
        vetNomField.clear(); // ✅ vider aussi le champ du nom
        statutBox.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur saisie");
        alert.setHeaderText("Contrôle de saisie");
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