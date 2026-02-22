package controllers;

import application.AppContext;
import application.service.ManagerDashboardService;
import entities.Hotel;
import entities.Reservation;
import entities.ReservationStatus;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import services.AuthorizationException;
import services.SessionContext;
import javafx.util.Duration;

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
    private ListView<Hotel> hotelListView;

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
    private TableColumn<Reservation, ReservationStatus> reservationStatusColumn;
    @FXML
    private TableColumn<Reservation, Reservation> reservationApproveColumn;
    @FXML
    private TableColumn<Reservation, Reservation> reservationDeclineColumn;

    private final ObservableList<Hotel> hotels = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    private ManagerDashboardService managerDashboardService;
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        User manager;
        try {
            manager = SessionContext.requireManager();
        } catch (AuthorizationException e) {
            Platform.runLater(() -> redirectToRoleSelection());
            return;
        }

        String managerIdentifier = manager.getPrincipalId() == null ? manager.getDisplayName() : manager.getPrincipalId();
        sessionLabel.setText("Role: HOTEL_MANAGER | " + managerIdentifier);

        try {
            managerDashboardService = AppContext.getInstance().managerDashboardService();
        } catch (RuntimeException e) {
            showError("Service initialization failed.");
            return;
        }

        configureHotelList();
        configureReservationTable();
        configureReservationActionColumns();
        configureHotelSelection();

        try {
            refreshData();
            startAutoRefresh();
        } catch (RuntimeException e) {
            showError("Database connection failed.");
        }
    }

    private void configureHotelList() {
        hotelListView.setItems(hotels);
        hotelListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Hotel hotel, boolean empty) {
                super.updateItem(hotel, empty);
                if (empty || hotel == null) {
                    setText(null);
                    return;
                }
                setText("ID: " + hotel.getId()
                        + " | " + hotel.getName()
                        + " | Address: " + hotel.getAddress()
                        + " | Manager ID: " + hotel.getManagerId()
                        + " | Capacity: " + hotel.getCapacity());
            }
        });
    }

    private void configureReservationTable() {
        reservationIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        reservationClientIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getClientId()).asObject());
        reservationAnimalIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAnimalId()).asObject());
        reservationHotelIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getHotelId()).asObject());
        reservationStartDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartDate()));
        reservationEndDateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndDate()));
        reservationStatusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        reservationStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ReservationStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-pending", "status-approved", "status-declined");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.name());
                switch (item) {
                    case APPROVED -> getStyleClass().add("status-approved");
                    case DECLINED -> getStyleClass().add("status-declined");
                    default -> getStyleClass().add("status-pending");
                }
            }
        });
        reservationTable.setItems(reservations);
    }

    private void configureReservationActionColumns() {
        reservationApproveColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        reservationDeclineColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        reservationApproveColumn.setCellFactory(column -> buildActionCell("Approve", true));
        reservationDeclineColumn.setCellFactory(column -> buildActionCell("Decline", false));
    }

    private TableCell<Reservation, Reservation> buildActionCell(String buttonLabel, boolean approve) {
        return new TableCell<>() {
            private final Button actionButton = createActionButton(buttonLabel, approve);

            {
                actionButton.setOnAction(event -> handleReservationDecision(approve));
            }

            @Override
            protected void updateItem(Reservation reservation, boolean empty) {
                super.updateItem(reservation, empty);
                if (empty || reservation == null) {
                    setGraphic(null);
                    return;
                }
                actionButton.setDisable(reservation.getStatus() != ReservationStatus.PENDING);
                setGraphic(actionButton);
            }

            private void handleReservationDecision(boolean approveAction) {
                Reservation selected = getTableView().getItems().get(getIndex());
                if (selected == null) {
                    return;
                }
                boolean success;
                try {
                    success = approveAction
                            ? managerDashboardService.approveReservation(selected.getId())
                            : managerDashboardService.declineReservation(selected.getId());
                } catch (AuthorizationException e) {
                    showError(e.getMessage());
                    return;
                }

                if (!success) {
                    showError("Action rejected. Reservation may already be processed.");
                } else {
                    clearError();
                }
                refreshReservations();
            }
        };
    }

    private Button createActionButton(String buttonLabel, boolean approve) {
        Button button = new Button(buttonLabel);
        button.getStyleClass().add("button");
        button.getStyleClass().add(approve ? "primary-button" : "danger-button");
        return button;
    }

    private void configureHotelSelection() {
        hotelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
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

            if (!managerDashboardService.addHotel(hotel)) {
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

            if (!managerDashboardService.updateHotel(hotel)) {
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
            if (!managerDashboardService.deleteHotel(id)) {
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
        hotelListView.getSelectionModel().clearSelection();
        clearError();
    }

    @FXML
    private void handleRefreshData() {
        refreshData();
    }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        SessionContext.logout();
        redirectToRoleSelection();
    }

    private void refreshData() {
        refreshHotels();
        refreshReservations();
    }

    private void refreshHotels() {
        hotels.setAll(managerDashboardService.getHotels());
    }

    private void refreshReservations() {
        reservations.setAll(managerDashboardService.getReservationsForManager());
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

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(20), event -> refreshReservations()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    private void showError(String message) {
        hotelErrorLabel.setText(message);
    }

    private void clearError() {
        hotelErrorLabel.setText("");
    }

    private void redirectToRoleSelection() {
        if (rootPane.getScene() == null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    redirectToRoleSelection();
                }
            });
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RoleSelection.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FurHope - Access Portal");
            stage.show();
        } catch (IOException e) {
            showError("Unable to return to access portal.");
        }
    }
}
