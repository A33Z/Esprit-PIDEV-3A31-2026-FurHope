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
        // Initialisation des statuts
        statutBox.setItems(FXCollections.observableArrayList(Disponibilite.Statut.values()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        // Lier les colonnes du tableau
        colDispoId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId_disponibilite()).asObject());
        colVetId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colStart.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStarttime().format(formatter)
                )
        );        colEnd.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEndtime().format(formatter)
                )
        );
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut().name()));

        // Quand on clique sur une ligne du tableau
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
            showInfo("Disponibilité ajoutée avec succès.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ✏️ Modifier
    @FXML
    private void onUpdate() {
        Disponibilite selected = disponibiliteTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une disponibilité à modifier.");
            return;
        }

        try {
            Disponibilite disponibilite = readFromForm();
            disponibilite.setId_disponibilite(selected.getId_disponibilite());
            serviceDisponibilite.update(disponibilite);
            showInfo("Disponibilité modifiée avec succès.");
            clearForm();
            refreshTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // 🗑 Supprimer
    @FXML
    private void onDelete() {
        Disponibilite selected = disponibiliteTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une disponibilité à supprimer.");
            return;
        }

        try {
            serviceDisponibilite.delete(selected.getId_disponibilite());
            showInfo("Disponibilité supprimée avec succès.");
            clearForm();
            refreshTable();
        } catch (SQLException e) {
            showError("Suppression impossible : " + e.getMessage());
        }
    }

    // 🔄 Vider le formulaire
    @FXML
    private void onClear() {
        clearForm();
    }

    // 🔍 Lecture des données saisies dans le formulaire
    private Disponibilite readFromForm() {
        int vetId = ValidationUtils.parsePositiveInt(vetIdField.getText(), "Vet ID");
        LocalDate date = ValidationUtils.requireDate(dateField.getValue(), "Date");
        LocalTime startTime = ValidationUtils.parseHourMinute(startTimeField.getText(), "Heure début");
        LocalTime endTime = ValidationUtils.parseHourMinute(endTimeField.getText(), "Heure fin");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début.");
        }

        Disponibilite.Statut statut = statutBox.getValue();
        if (statut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire.");
        }

        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        Disponibilite disponibilite = new Disponibilite(vetId, startDateTime, endDateTime, statut);
        return disponibilite;
    }
    private void fillForm(Disponibilite disponibilite) {
        vetIdField.setText(String.valueOf(disponibilite.getId()));
        statutBox.setValue(disponibilite.getStatut());

        try {
            LocalDateTime start = disponibilite.getStarttime();
            LocalDateTime end = disponibilite.getEndtime();
            dateField.setValue(start.toLocalDate());
            startTimeField.setText(start.toLocalTime().toString().substring(0, 5));
            endTimeField.setText(end.toLocalTime().toString().substring(0, 5));
        } catch (Exception ignored) {
            dateField.setValue(null);
            startTimeField.clear();
            endTimeField.clear();
        }
    }

    // Réinitialiser les champs
    private void clearForm() {
        disponibiliteTable.getSelectionModel().clearSelection();
        vetIdField.clear();
        dateField.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        statutBox.setValue(null);
    }

    // Recharger les données du tableau
    private void refreshTable() {
        try {
            disponibiliteTable.setItems(FXCollections.observableArrayList(serviceDisponibilite.read()));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    // Afficher une alerte d’erreur
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Contrôle de saisie / Opération");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Afficher une alerte d’information
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}