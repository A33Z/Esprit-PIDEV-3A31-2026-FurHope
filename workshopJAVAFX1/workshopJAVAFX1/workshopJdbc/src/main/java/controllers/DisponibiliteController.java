package controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import services.ServiceDisponibilite;
import utils.ValidationUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DisponibiliteController {

    @FXML
    private TableView<Disponibilite> disponibiliteTable;
    @FXML
    private TableColumn<Disponibilite, Integer> colDispoId;
    @FXML
    private TableColumn<Disponibilite, Integer> colVetId;
    @FXML
    private TableColumn<Disponibilite, String> colStart;
    @FXML
    private TableColumn<Disponibilite, String> colEnd;
    @FXML
    private TableColumn<Disponibilite, String> colStatut;

    @FXML
    private TextField vetIdField;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private ComboBox<Disponibilite.Statut> statutBox;

    private final ServiceDisponibilite serviceDisponibilite = new ServiceDisponibilite();
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statutBox.setItems(FXCollections.observableArrayList(Disponibilite.Statut.values()));

        colDispoId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId_disponibilite()).asObject());
        colVetId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colStart.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStarttime()));
        colEnd.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndtime()));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut().name()));

        disponibiliteTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });

        refreshTable();
    }

    @FXML
    private void onCreate() {
        try {
            Disponibilite disponibilite = readFromForm();
            serviceDisponibilite.add(disponibilite);
            showInfo("Disponibilite ajoutee avec succes.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        Disponibilite selected = disponibiliteTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selectionnez une disponibilite a modifier.");
            return;
        }

        try {
            Disponibilite disponibilite = readFromForm();
            disponibilite.setId_disponibilite(selected.getId_disponibilite());
            serviceDisponibilite.update(disponibilite);
            showInfo("Disponibilite modifiee avec succes.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Disponibilite selected = disponibiliteTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selectionnez une disponibilite a supprimer.");
            return;
        }

        try {
            serviceDisponibilite.delete(selected.getId_disponibilite());
            showInfo("Disponibilite supprimee avec succes.");
            clearForm();
            refreshTable();
        } catch (SQLException e) {
            showError("Suppression impossible: " + e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private Disponibilite readFromForm() {
        int vetId = ValidationUtils.parsePositiveInt(vetIdField.getText(), "Vet ID");
        LocalDate date = ValidationUtils.requireDate(dateField.getValue(), "Date");
        LocalTime startTime = ValidationUtils.parseHourMinute(startTimeField.getText(), "Heure debut");
        LocalTime endTime = ValidationUtils.parseHourMinute(endTimeField.getText(), "Heure fin");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Heure fin doit etre apres heure debut.");
        }

        Disponibilite.Statut statut = statutBox.getValue();
        if (statut == null) {
            throw new IllegalArgumentException("Statut est obligatoire.");
        }

        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

        return new Disponibilite(vetId, startDateTime.format(DATETIME_FORMAT), endDateTime.format(DATETIME_FORMAT), statut);
    }

    private void fillForm(Disponibilite disponibilite) {
        vetIdField.setText(String.valueOf(disponibilite.getId()));
        statutBox.setValue(disponibilite.getStatut());

        try {
            LocalDateTime start = LocalDateTime.parse(disponibilite.getStarttime(), DATETIME_FORMAT);
            LocalDateTime end = LocalDateTime.parse(disponibilite.getEndtime(), DATETIME_FORMAT);
            dateField.setValue(start.toLocalDate());
            startTimeField.setText(start.toLocalTime().toString().substring(0, 5));
            endTimeField.setText(end.toLocalTime().toString().substring(0, 5));
        } catch (Exception ignored) {
            dateField.setValue(null);
            startTimeField.clear();
            endTimeField.clear();
        }
    }

    private void clearForm() {
        disponibiliteTable.getSelectionModel().clearSelection();
        vetIdField.clear();
        dateField.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        statutBox.setValue(null);
    }

    private void refreshTable() {
        try {
            disponibiliteTable.setItems(FXCollections.observableArrayList(serviceDisponibilite.read()));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
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
