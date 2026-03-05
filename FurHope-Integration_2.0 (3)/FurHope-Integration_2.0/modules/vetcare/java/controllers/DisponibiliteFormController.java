package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import services.ServiceDisponibilite;
import utils.EditState;
import utils.SessionManager;
import utils.ValidationUtils;
import utils.ViewNavigator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DisponibiliteFormController {

    @FXML private Label titleLabel;
    @FXML private DatePicker dateField;

    // ✅ Spinners au lieu de TextFields
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinSpinner;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinSpinner;

    @FXML private ComboBox<Disponibilite.Statut> statutBox;
    @FXML private Button saveButton;

    private final ServiceDisponibilite service = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        statutBox.setItems(FXCollections.observableArrayList(Disponibilite.Statut.values()));

        // ✅ Initialiser les Spinners
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        startMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10));
        endMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        if (EditState.disponibiliteToEdit != null) {
            titleLabel.setText("✏️ Modifier Disponibilité");
            saveButton.setText("💾 Modifier");
            fillForm(EditState.disponibiliteToEdit);
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        try {
            int vetId = SessionManager.getUserId();
            LocalDate date = ValidationUtils.requireDate(dateField.getValue(), "Date");

            // ✅ Lire les heures depuis les Spinners
            int startH = startHourSpinner.getValue();
            int startM = startMinSpinner.getValue();
            int endH   = endHourSpinner.getValue();
            int endM   = endMinSpinner.getValue();

            LocalTime startTime = LocalTime.of(startH, startM);
            LocalTime endTime   = LocalTime.of(endH, endM);

            if (!endTime.isAfter(startTime)) {
                showError("L'heure de fin doit être après l'heure de début.");
                return;
            }

            Disponibilite.Statut statut = statutBox.getValue();
            if (statut == null) {
                showError("Le statut est obligatoire.");
                return;
            }

            LocalDateTime start = LocalDateTime.of(date, startTime);
            LocalDateTime end   = LocalDateTime.of(date, endTime);

            Disponibilite dispo = new Disponibilite(vetId, start, end, statut);

            if (EditState.disponibiliteToEdit != null) {
                dispo.setId_disponibilite(EditState.disponibiliteToEdit.getId_disponibilite());
                service.update(dispo);
                showInfo("✅ Disponibilité modifiée !");
                EditState.disponibiliteToEdit = null;
            } else {
                service.add(dispo);
                showInfo("✅ Disponibilité ajoutée !");
            }

            clearForm();
            ViewNavigator.goTo(event, "/DisponibiliteList.fxml", "Mes Disponibilités");

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onGoList(ActionEvent event) {
        ViewNavigator.goTo(event, "/DisponibiliteList.fxml", "Mes Disponibilités");
    }

    @FXML
    private void onGoHome(ActionEvent event) {
        ViewNavigator.goTo(event, "/Dashboard.fxml", "Dashboard Vétérinaire");
    }
    @FXML
    private void onBackAccueil(ActionEvent event) {
        ViewNavigator.goTo(event, "/accueil.fxml", "FurHope");
    }

    private void fillForm(Disponibilite d) {
        try {
            dateField.setValue(d.getStarttime().toLocalDate());
            startHourSpinner.getValueFactory().setValue(d.getStarttime().getHour());
            startMinSpinner.getValueFactory().setValue(d.getStarttime().getMinute());
            endHourSpinner.getValueFactory().setValue(d.getEndtime().getHour());
            endMinSpinner.getValueFactory().setValue(d.getEndtime().getMinute());
            statutBox.setValue(d.getStatut());
        } catch (Exception ignored) {}
    }

    private void clearForm() {
        dateField.setValue(null);
        startHourSpinner.getValueFactory().setValue(8);
        startMinSpinner.getValueFactory().setValue(0);
        endHourSpinner.getValueFactory().setValue(10);
        endMinSpinner.getValueFactory().setValue(0);
        statutBox.setValue(null);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
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