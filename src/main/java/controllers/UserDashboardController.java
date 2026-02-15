package controllers;

import entities.Hotel;
import entities.Reservation;
import entities.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import security.AuthorizationException;
import security.SessionContext;
import services.HotelAccessService;
import services.ReservationAccessService;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

public class UserDashboardController {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label sessionLabel;
    @FXML
    private Label reservationErrorLabel;

    @FXML
    private TableView<Hotel> hotelTable;
    @FXML
    private TableColumn<Hotel, Integer> hotelIdColumn;
    @FXML
    private TableColumn<Hotel, String> hotelNameColumn;
    @FXML
    private TableColumn<Hotel, String> hotelAddressColumn;
    @FXML
    private TableColumn<Hotel, Integer> hotelManagerIdColumn;
    @FXML
    private TableColumn<Hotel, Integer> hotelCapacityColumn;

    @FXML
    private TextField reservationIdField;
    @FXML
    private TextField animalIdField;
    @FXML
    private TextField reservationHotelIdField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TableView<Reservation> myReservationTable;
    @FXML
    private TableColumn<Reservation, Integer> myReservationIdColumn;
    @FXML
    private TableColumn<Reservation, Integer> myAnimalIdColumn;
    @FXML
    private TableColumn<Reservation, Integer> myHotelIdColumn;
    @FXML
    private TableColumn<Reservation, Date> myStartDateColumn;
    @FXML
    private TableColumn<Reservation, Date> myEndDateColumn;
    @FXML
    private TableColumn<Reservation, String> myStatusColumn;

    private final ObservableList<Hotel> hotels = FXCollections.observableArrayList();
    private final ObservableList<Reservation> myReservations = FXCollections.observableArrayList();

    private HotelAccessService hotelAccessService;
    private ReservationAccessService reservationAccessService;

    @FXML
    public void initialize() {
        User user = SessionContext.requireUser();
        sessionLabel.setText("Logged in as: " + user.getDisplayName());

        configureHotelTable();
        configureReservationTable();
        configureReservationSelection();
        statusComboBox.setItems(FXCollections.observableArrayList("PENDING", "CONFIRMED", "CANCELLED"));

        try {
            hotelAccessService = new HotelAccessService();
            reservationAccessService = new ReservationAccessService();
            refreshData();
        } catch (RuntimeException e) {
            showError("Database connection failed.");
        }
    }

    private void configureHotelTable() {
        hotelIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        hotelNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        hotelAddressColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAddress()));
        hotelManagerIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getManagerId()).asObject());
        hotelCapacityColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacity()).asObject());
        hotelTable.setItems(hotels);
    }

    private void configureReservationTable() {
        myReservationIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        myAnimalIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAnimalId()).asObject());
        myHotelIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getHotelId()).asObject());
        myStartDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartDate()));
        myEndDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndDate()));
        myStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        myReservationTable.setItems(myReservations);
    }

    private void configureReservationSelection() {
        myReservationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                return;
            }
            reservationIdField.setText(String.valueOf(selected.getId()));
            animalIdField.setText(String.valueOf(selected.getAnimalId()));
            reservationHotelIdField.setText(String.valueOf(selected.getHotelId()));
            startDatePicker.setValue(toLocalDate(selected.getStartDate()));
            endDatePicker.setValue(toLocalDate(selected.getEndDate()));
            statusComboBox.setValue(selected.getStatus());
            clearError();
        });
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    @FXML
    private void handleAddReservation() {
        try {
            Reservation reservation = new Reservation(
                    0,
                    parsePositiveInt(animalIdField.getText(), "Animal ID"),
                    parsePositiveInt(reservationHotelIdField.getText(), "Hotel ID"),
                    toSqlDate(startDatePicker.getValue(), "Start date"),
                    toSqlDate(endDatePicker.getValue(), "End date"),
                    parseStatus()
            );

            if (!reservationAccessService.addReservationForCurrentUser(reservation)) {
                showError("Add reservation failed.");
                return;
            }
            handleClearReservationForm();
            refreshMyReservations();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleUpdateReservation() {
        try {
            Reservation reservation = new Reservation(
                    parsePositiveInt(reservationIdField.getText(), "Reservation ID"),
                    0,
                    parsePositiveInt(animalIdField.getText(), "Animal ID"),
                    parsePositiveInt(reservationHotelIdField.getText(), "Hotel ID"),
                    toSqlDate(startDatePicker.getValue(), "Start date"),
                    toSqlDate(endDatePicker.getValue(), "End date"),
                    parseStatus()
            );

            if (!reservationAccessService.updateReservationForCurrentUser(reservation)) {
                showError("Update failed. You can only update your own reservations.");
                return;
            }
            refreshMyReservations();
            clearError();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteReservation() {
        try {
            int reservationId = parsePositiveInt(reservationIdField.getText(), "Reservation ID");
            if (!reservationAccessService.deleteReservationForCurrentUser(reservationId)) {
                showError("Delete failed. You can only delete your own reservations.");
                return;
            }
            handleClearReservationForm();
            refreshMyReservations();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleClearReservationForm() {
        reservationIdField.clear();
        animalIdField.clear();
        reservationHotelIdField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        statusComboBox.setValue(null);
        myReservationTable.getSelectionModel().clearSelection();
        clearError();
    }

    @FXML
    private void handleRefreshData() {
        refreshData();
    }

    @FXML
    private void handleLogout() {
        SessionContext.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RoleSelection.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FurHope - Role Selection");
            stage.show();
        } catch (IOException e) {
            showError("Unable to return to role selection.");
        }
    }

    private void refreshData() {
        hotels.setAll(hotelAccessService.viewAllHotels());
        refreshMyReservations();
    }

    private void refreshMyReservations() {
        myReservations.setAll(reservationAccessService.viewCurrentUserReservations());
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be > 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
    }

    private Date toSqlDate(LocalDate localDate, String fieldName) {
        if (localDate == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return Date.valueOf(localDate);
    }

    private String parseStatus() {
        String status = statusComboBox.getValue();
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required.");
        }
        return status.trim();
    }

    private void showError(String message) {
        reservationErrorLabel.setText(message);
    }

    private void clearError() {
        reservationErrorLabel.setText("");
    }
}
