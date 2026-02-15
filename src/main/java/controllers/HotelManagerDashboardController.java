package controllers;

import entities.Hotel;
import entities.Reservation;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

public class HotelManagerDashboardController {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label sessionLabel;
    @FXML
    private Label hotelErrorLabel;
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField managerIdField;
    @FXML
    private TextField capacityField;

    @FXML
    private TableView<Hotel> hotelTable;
    @FXML
    private TableColumn<Hotel, Integer> idColumn;
    @FXML
    private TableColumn<Hotel, String> nameColumn;
    @FXML
    private TableColumn<Hotel, String> addressColumn;
    @FXML
    private TableColumn<Hotel, Integer> managerIdColumn;
    @FXML
    private TableColumn<Hotel, Integer> capacityColumn;

    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private TableColumn<Reservation, Integer> reservationIdColumn;
    @FXML
    private TableColumn<Reservation, Integer> reservationClientIdColumn;
    @FXML
    private TableColumn<Reservation, Integer> reservationAnimalIdColumn;
    @FXML
    private TableColumn<Reservation, Integer> reservationHotelIdColumn;
    @FXML
    private TableColumn<Reservation, Date> reservationStartDateColumn;
    @FXML
    private TableColumn<Reservation, Date> reservationEndDateColumn;
    @FXML
    private TableColumn<Reservation, String> reservationStatusColumn;

    private final ObservableList<Hotel> hotels = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    private HotelAccessService hotelAccessService;
    private ReservationAccessService reservationAccessService;

    @FXML
    public void initialize() {
        sessionLabel.setText("Role: HOTEL_MANAGER");
        configureHotelTable();
        configureReservationTable();
        configureHotelSelection();

        try {
            hotelAccessService = new HotelAccessService();
            reservationAccessService = new ReservationAccessService();
            refreshData();
        } catch (RuntimeException e) {
            showError("Database connection failed.");
        }
    }

    private void configureHotelTable() {
        idColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        addressColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAddress()));
        managerIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getManagerId()).asObject());
        capacityColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCapacity()).asObject());
        hotelTable.setItems(hotels);
    }

    private void configureReservationTable() {
        reservationIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        reservationClientIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getClientId()).asObject());
        reservationAnimalIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAnimalId()).asObject());
        reservationHotelIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getHotelId()).asObject());
        reservationStartDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartDate()));
        reservationEndDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndDate()));
        reservationStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        reservationTable.setItems(reservations);
    }

    private void configureHotelSelection() {
        hotelTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                return;
            }
            idField.setText(String.valueOf(selected.getId()));
            nameField.setText(selected.getName());
            addressField.setText(selected.getAddress());
            managerIdField.setText(String.valueOf(selected.getManagerId()));
            capacityField.setText(String.valueOf(selected.getCapacity()));
            clearError();
        });
    }

    @FXML
    private void handleAddHotel() {
        try {
            Hotel hotel = new Hotel(
                    requireText(nameField.getText(), "Name"),
                    requireText(addressField.getText(), "Address"),
                    parseNonNegativeInt(managerIdField.getText(), "Manager ID"),
                    parseNonNegativeInt(capacityField.getText(), "Capacity")
            );

            if (!hotelAccessService.addHotel(hotel)) {
                showError("Add hotel failed.");
                return;
            }
            handleClearHotelForm();
            refreshHotels();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleUpdateHotel() {
        try {
            Hotel hotel = new Hotel(
                    parsePositiveInt(idField.getText(), "Hotel ID"),
                    requireText(nameField.getText(), "Name"),
                    requireText(addressField.getText(), "Address"),
                    parseNonNegativeInt(managerIdField.getText(), "Manager ID"),
                    parseNonNegativeInt(capacityField.getText(), "Capacity")
            );

            if (!hotelAccessService.updateHotel(hotel)) {
                showError("Update hotel failed. Check ID.");
                return;
            }
            refreshHotels();
            clearError();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteHotel() {
        try {
            int id = parsePositiveInt(idField.getText(), "Hotel ID");
            if (!hotelAccessService.deleteHotel(id)) {
                showError("Delete hotel failed. Check ID.");
                return;
            }
            handleClearHotelForm();
            refreshHotels();
        } catch (IllegalArgumentException | AuthorizationException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleClearHotelForm() {
        idField.clear();
        nameField.clear();
        addressField.clear();
        managerIdField.clear();
        capacityField.clear();
        hotelTable.getSelectionModel().clearSelection();
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
        refreshHotels();
        refreshReservations();
    }

    private void refreshHotels() {
        hotels.setAll(hotelAccessService.viewAllHotels());
    }

    private void refreshReservations() {
        reservations.setAll(reservationAccessService.viewAllReservationsForManager());
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

    private int parseNonNegativeInt(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " must be >= 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
    }

    private String requireText(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return rawValue.trim();
    }

    private void showError(String message) {
        hotelErrorLabel.setText(message);
    }

    private void clearError() {
        hotelErrorLabel.setText("");
    }
}
