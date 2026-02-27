package controllers;

import application.AppContext;
import application.model.ManagerHotelInfoModel;
import application.model.ManagerReservationTicketModel;
import application.service.ManagerDashboardService;
import application.ui.HotelInfoCard;
import application.ui.ReservationTicketCard;
import entities.Hotel;
import entities.ReservationStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import services.AuthorizationException;
import services.ReservationDecisionCode;
import services.ReservationDecisionResult;
import services.SessionContext;
import services.UserReservationActionCode;
import services.UserReservationActionResult;
import utils.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HotelManagerDashboardController {

    private static final String ALL_LOCATIONS = "All locations";
    private static final String ALL_RATINGS = "All ratings";
    private static final String ALL_AVAILABILITY = "All availability";

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);

    @FXML
    private AnchorPane rootPane;
    @FXML
    private GridPane dashboardGrid;

    @FXML
    private VBox allHotelsPanel;
    @FXML
    private VBox reservationPanel;
    @FXML
    private VBox resourcePanel;

    @FXML
    private Label sessionLabel;

    // All hotels
    @FXML
    private TextField hotelSearchField;
    @FXML
    private ComboBox<String> locationFilterCombo;
    @FXML
    private ComboBox<String> ratingFilterCombo;
    @FXML
    private ComboBox<String> availabilityFilterCombo;
    @FXML
    private Label hotelResultsLabel;
    @FXML
    private ScrollPane hotelCardsScrollPane;
    @FXML
    private VBox hotelCardContainer;

    // Reservations
    @FXML
    private ListView<ManagerReservationTicketModel> reservationTicketListView;
    @FXML
    private Label reservationSummaryLabel;

    // Hotel management form
    @FXML
    private TextField hotelIdField;
    @FXML
    private TextField hotelNameField;
    @FXML
    private TextField hotelAddressField;
    @FXML
    private TextField hotelManagerIdField;
    @FXML
    private TextField hotelCapacityField;
    @FXML
    private TextField hotelCreatedAtField;
    @FXML
    private Label resourceErrorLabel;
    @FXML
    private Label resourceSuccessLabel;

    private ManagerDashboardService managerDashboardService;

    private final Map<Integer, Hotel> hotelsById = new HashMap<>();

    private List<ManagerHotelInfoModel> allHotels = List.of();

    private Timeline autoRefreshTimeline;
    private boolean compactLayoutEnabled;

    @FXML
    public void initialize() {
        try {
            SessionContext.requireManager();
        } catch (AuthorizationException e) {
            Platform.runLater(this::redirectToRoleSelection);
            return;
        }

        try {
            managerDashboardService = AppContext.getInstance().managerDashboardService();
        } catch (RuntimeException e) {
            managerDashboardService = null;
        }

        try {
            var manager = SessionContext.requireManager();
            sessionLabel.setText("Logged in as: " + manager.getDisplayName());
        } catch (RuntimeException ignored) {
            sessionLabel.setText("Manager session");
        }

        configureReservationListView();
        configureHotelFilters();
        configureResourceForm();
        configureResponsiveLayout();

        refreshAll();
        startAutoRefresh();
    }

    private void configureReservationListView() {
        reservationTicketListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ManagerReservationTicketModel ticket, boolean empty) {
                super.updateItem(ticket, empty);
                if (empty || ticket == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(new ReservationTicketCard(
                        ticket,
                        () -> handleModifyReservation(ticket),
                        () -> handleCancelReservation(ticket),
                        () -> handleApproveReservation(ticket),
                        () -> handleDeclineReservation(ticket)
                ));
                setText(null);
            }
        });
    }

    private void configureHotelFilters() {
        locationFilterCombo.getItems().setAll(ALL_LOCATIONS);
        locationFilterCombo.setValue(ALL_LOCATIONS);

        ratingFilterCombo.getItems().setAll(
                ALL_RATINGS,
                "5.0",
                "4.5+",
                "4.0+",
                "3.5+"
        );
        ratingFilterCombo.setValue(ALL_RATINGS);

        availabilityFilterCombo.getItems().setAll(
                ALL_AVAILABILITY,
                "Open",
                "Available",
                "Limited",
                "Nearly Full",
                "Unknown"
        );
        availabilityFilterCombo.setValue(ALL_AVAILABILITY);

        hotelSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderHotelCards());
        locationFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderHotelCards());
        ratingFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderHotelCards());
        availabilityFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderHotelCards());
    }

    private void configureResourceForm() {
        hotelIdField.setEditable(false);
        hotelCreatedAtField.setEditable(false);

        hotelCapacityField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}") ? change : null
        ));

        hotelManagerIdField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,10}") ? change : null
        ));
    }

    private void configureResponsiveLayout() {
        if (rootPane == null) {
            return;
        }
        rootPane.widthProperty().addListener((obs, oldWidth, newWidth) -> applyResponsiveLayout(newWidth.doubleValue()));
        Platform.runLater(() -> applyResponsiveLayout(rootPane.getWidth()));
    }

    private void applyResponsiveLayout(double width) {
        if (dashboardGrid == null) {
            return;
        }

        boolean compact = width < 1240;
        if (compact == compactLayoutEnabled) {
            return;
        }
        compactLayoutEnabled = compact;

        dashboardGrid.getColumnConstraints().clear();
        dashboardGrid.getRowConstraints().clear();

        if (compact) {
            dashboardGrid.getColumnConstraints().add(growColumn(100.0));
            dashboardGrid.getRowConstraints().addAll(
                    growRow(33.4),
                    growRow(33.3),
                    growRow(33.3)
            );

            placePanel(allHotelsPanel, 0, 0);
            placePanel(reservationPanel, 0, 1);
            placePanel(resourcePanel, 0, 2);
            GridPane.setRowSpan(allHotelsPanel, 1);
            GridPane.setRowSpan(reservationPanel, 1);
            GridPane.setRowSpan(resourcePanel, 1);
            return;
        }

        dashboardGrid.getColumnConstraints().addAll(growColumn(50.0), growColumn(50.0));
        dashboardGrid.getRowConstraints().addAll(growRow(46.0), growRow(54.0));

        placePanel(allHotelsPanel, 0, 0);
        placePanel(resourcePanel, 1, 0);
        placePanel(reservationPanel, 0, 1);
        GridPane.setRowSpan(allHotelsPanel, 1);
        GridPane.setRowSpan(reservationPanel, 1);
        GridPane.setRowSpan(resourcePanel, 2);
    }

    private ColumnConstraints growColumn(double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setHgrow(Priority.ALWAYS);
        constraints.setPercentWidth(percentWidth);
        return constraints;
    }

    private RowConstraints growRow(double percentHeight) {
        RowConstraints constraints = new RowConstraints();
        constraints.setVgrow(Priority.ALWAYS);
        constraints.setPercentHeight(percentHeight);
        return constraints;
    }

    private void placePanel(Node panel, int column, int row) {
        if (panel == null) {
            return;
        }
        GridPane.setColumnIndex(panel, column);
        GridPane.setRowIndex(panel, row);
        GridPane.setHgrow(panel, Priority.ALWAYS);
        GridPane.setVgrow(panel, Priority.ALWAYS);
    }

    @FXML
    private void handleRefreshData() {
        refreshAll();
    }

    @FXML
    private void handleOpenAnalytics() {
        try {
            SessionContext.requireManager();
        } catch (AuthorizationException e) {
            redirectToRoleSelection();
            return;
        }

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        navigateTo("/Analytics.fxml", "FurHope - Analytics");
    }

    @FXML
    private void handleLogout() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        SessionContext.logout();
        redirectToRoleSelection();
    }

    private void refreshAll() {
        refreshHotels();
        refreshReservations();
        refreshResources();
    }

    private void refreshHotels() {
        if (managerDashboardService == null) {
            allHotels = List.of();
            hotelsById.clear();
            hotelCardContainer.getChildren().setAll(emptyStateLabel("Hotel data is currently unavailable."));
            hotelResultsLabel.setText("Showing 0 of 0 hotels");
            return;
        }

        try {
            List<Hotel> hotels = managerDashboardService.getHotels();
            updateHotelLookup(hotels);
            allHotels = managerDashboardService.getHotelInfoModelsForManager();
            updateLocationFilterItems();
            renderHotelCards();
            refreshResources();
        } catch (RuntimeException e) {
            allHotels = List.of();
            hotelsById.clear();
            hotelCardContainer.getChildren().setAll(emptyStateLabel("Could not load hotels."));
            hotelResultsLabel.setText("Showing 0 of 0 hotels");
        }
    }

    private void updateLocationFilterItems() {
        String currentSelection = locationFilterCombo.getValue();

        List<String> locations = allHotels.stream()
                .map(ManagerHotelInfoModel::location)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> options = new ArrayList<>();
        options.add(ALL_LOCATIONS);
        options.addAll(locations);
        locationFilterCombo.getItems().setAll(options);

        if (currentSelection != null && options.contains(currentSelection)) {
            locationFilterCombo.setValue(currentSelection);
        } else {
            locationFilterCombo.setValue(ALL_LOCATIONS);
        }
    }

    private void updateHotelLookup(List<Hotel> hotels) {
        hotelsById.clear();
        if (hotels == null || hotels.isEmpty()) {
            return;
        }
        for (Hotel hotel : hotels) {
            if (hotel != null && hotel.getId() > 0) {
                hotelsById.put(hotel.getId(), hotel);
            }
        }
    }

    private void renderHotelCards() {
        if (hotelCardContainer == null) {
            return;
        }

        List<ManagerHotelInfoModel> filteredHotels = allHotels.stream()
                .filter(this::matchesHotelFilters)
                .toList();

        hotelCardContainer.getChildren().clear();

        if (filteredHotels.isEmpty()) {
            hotelCardContainer.getChildren().add(emptyStateLabel("No hotels match the current filters."));
        } else {
            for (ManagerHotelInfoModel hotel : filteredHotels) {
                HotelInfoCard card = new HotelInfoCard(hotel, () -> focusResourcePanelForHotel(hotel.hotelId()));
                card.setMaxWidth(Double.MAX_VALUE);
                hotelCardContainer.getChildren().add(card);
            }
        }

        hotelResultsLabel.setText(String.format(
                Locale.US,
                "Showing %d of %d hotels",
                filteredHotels.size(),
                allHotels.size()
        ));
    }

    private boolean matchesHotelFilters(ManagerHotelInfoModel hotel) {
        if (hotel == null) {
            return false;
        }

        String query = normalize(hotelSearchField.getText()).toLowerCase(Locale.US);
        if (!query.isBlank()) {
            String hotelName = normalize(hotel.hotelName()).toLowerCase(Locale.US);
            String location = normalize(hotel.location()).toLowerCase(Locale.US);
            if (!hotelName.contains(query) && !location.contains(query)) {
                return false;
            }
        }

        String selectedLocation = locationFilterCombo.getValue();
        if (selectedLocation != null
                && !ALL_LOCATIONS.equalsIgnoreCase(selectedLocation)
                && !selectedLocation.equalsIgnoreCase(hotel.location())) {
            return false;
        }

        String selectedAvailability = availabilityFilterCombo.getValue();
        if (selectedAvailability != null
                && !ALL_AVAILABILITY.equalsIgnoreCase(selectedAvailability)
                && !selectedAvailability.equalsIgnoreCase(hotel.availabilityStatus())) {
            return false;
        }

        return matchesRatingFilter(hotel.starRating(), ratingFilterCombo.getValue());
    }

    private boolean matchesRatingFilter(double rating, String ratingFilter) {
        if (ratingFilter == null || ratingFilter.isBlank() || ALL_RATINGS.equalsIgnoreCase(ratingFilter)) {
            return true;
        }
        return switch (ratingFilter) {
            case "5.0" -> rating >= 5.0;
            case "4.5+" -> rating >= 4.5;
            case "4.0+" -> rating >= 4.0;
            case "3.5+" -> rating >= 3.5;
            default -> true;
        };
    }

    @FXML
    private void handleClearHotelFilters() {
        hotelSearchField.clear();
        locationFilterCombo.setValue(ALL_LOCATIONS);
        ratingFilterCombo.setValue(ALL_RATINGS);
        availabilityFilterCombo.setValue(ALL_AVAILABILITY);
        renderHotelCards();
    }

    private void focusResourcePanelForHotel(int hotelId) {
        Hotel hotel = hotelsById.get(hotelId);
        if (hotel == null) {
            return;
        }
        populateHotelForm(hotel);
        showResourceSuccess("Form prefilled for " + safeDisplay(hotel.getName(), "selected hotel") + ".");
    }

    private void refreshReservations() {
        if (managerDashboardService == null) {
            reservationTicketListView.getItems().clear();
            reservationSummaryLabel.setText("Active reservations: 0");
            return;
        }

        try {
            List<ManagerReservationTicketModel> activeTickets = managerDashboardService.getActiveReservationTicketsForManager();
            reservationTicketListView.getItems().setAll(activeTickets);
            reservationSummaryLabel.setText("Active reservations: " + activeTickets.size());
        } catch (RuntimeException e) {
            reservationTicketListView.getItems().clear();
            reservationSummaryLabel.setText("Active reservations: 0");
        }
    }

    private void refreshResources() {
        Integer selectedHotelId = parsePositiveInteger(hotelIdField.getText());
        if (selectedHotelId == null) {
            return;
        }
        Hotel hotel = hotelsById.get(selectedHotelId);
        if (hotel != null) {
            populateHotelForm(hotel);
        }
    }

    private void populateHotelForm(Hotel hotel) {
        if (hotel == null) {
            return;
        }

        hotelIdField.setText(String.valueOf(hotel.getId()));
        hotelNameField.setText(safeDisplay(hotel.getName(), ""));
        hotelAddressField.setText(safeDisplay(hotel.getAddress(), ""));
        hotelManagerIdField.setText(hotel.getManagerId() > 0 ? String.valueOf(hotel.getManagerId()) : "");
        hotelCapacityField.setText(String.valueOf(Math.max(0, hotel.getCapacity())));
        hotelCreatedAtField.setText(resolveHotelCreatedAt(hotel.getId()));
        clearResourceFeedback();
    }

    private String resolveHotelCreatedAt(int hotelId) {
        if (hotelId <= 0) {
            return "";
        }

        String sql = "SELECT created_at FROM hotel WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hotelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    if (createdAt != null) {
                        return createdAt.toLocalDateTime().format(CREATED_AT_FORMAT);
                    }
                }
            }
        } catch (SQLException ignored) {
            // Legacy schemas may not include created_at; keep the field visible with a safe fallback.
        }
        return "Auto-generated by database";
    }

    @FXML
    private void handleAddResource() {
        clearResourceFeedback();
        if (managerDashboardService == null) {
            showResourceError("Service unavailable.");
            return;
        }

        HotelPayload payload = validateHotelPayload();
        if (payload == null) {
            return;
        }

        try {
            boolean added = managerDashboardService.addHotel(
                    new Hotel(
                            payload.name(),
                            payload.address(),
                            payload.managerId(),
                            payload.capacity()
                    )
            );
            if (!added) {
                showResourceError("Hotel could not be added.");
                return;
            }
            refreshHotels();
            handleClearResourceForm();
            showResourceSuccess("Hotel added successfully.");
        } catch (IllegalArgumentException e) {
            showResourceError(safeDisplay(e.getMessage(), "Invalid hotel input."));
        } catch (RuntimeException e) {
            showResourceError("Could not add hotel right now.");
        }
    }

    @FXML
    private void handleUpdateResource() {
        clearResourceFeedback();
        if (managerDashboardService == null) {
            showResourceError("Service unavailable.");
            return;
        }

        Integer hotelId = parsePositiveInteger(hotelIdField.getText());
        if (hotelId == null) {
            showResourceError("ID is required to update a hotel.");
            return;
        }

        HotelPayload payload = validateHotelPayload();
        if (payload == null) {
            return;
        }

        try {
            boolean updated = managerDashboardService.updateHotel(
                    new Hotel(
                            hotelId,
                            payload.name(),
                            payload.address(),
                            payload.managerId(),
                            payload.capacity()
                    )
            );
            if (!updated) {
                showResourceError("Hotel could not be updated.");
                return;
            }
            refreshHotels();
            focusResourcePanelForHotel(hotelId);
            showResourceSuccess("Hotel updated successfully.");
        } catch (IllegalArgumentException e) {
            showResourceError(safeDisplay(e.getMessage(), "Invalid hotel input."));
        } catch (RuntimeException e) {
            showResourceError("Could not update hotel right now.");
        }
    }

    @FXML
    private void handleDeleteResource() {
        clearResourceFeedback();
        if (managerDashboardService == null) {
            showResourceError("Service unavailable.");
            return;
        }

        Integer hotelId = parsePositiveInteger(hotelIdField.getText());
        if (hotelId == null) {
            showResourceError("ID is required to delete a hotel.");
            return;
        }

        String hotelName = safeDisplay(hotelNameField.getText(), "selected hotel");

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Hotel");
        confirmation.setHeaderText("Delete hotel?");
        confirmation.setContentText("This will permanently remove \"" + hotelName + "\".");

        Optional<ButtonType> decision = confirmation.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        try {
            boolean deleted = managerDashboardService.deleteHotel(hotelId);
            if (!deleted) {
                showResourceError("Hotel could not be deleted.");
                return;
            }
            refreshHotels();
            refreshReservations();
            handleClearResourceForm();
            showResourceSuccess("Hotel deleted successfully.");
        } catch (RuntimeException e) {
            showResourceError("Could not delete hotel right now.");
        }
    }

    @FXML
    private void handleClearResourceForm() {
        hotelIdField.clear();
        hotelNameField.clear();
        hotelAddressField.clear();
        hotelManagerIdField.clear();
        hotelCapacityField.clear();
        hotelCreatedAtField.clear();
        clearResourceFeedback();
    }

    private HotelPayload validateHotelPayload() {
        String name = normalize(hotelNameField.getText());
        if (name.isBlank()) {
            showResourceError("Name is required.");
            return null;
        }

        String address = normalize(hotelAddressField.getText());
        if (address.isBlank()) {
            showResourceError("Address is required.");
            return null;
        }

        String managerIdRaw = normalize(hotelManagerIdField.getText());
        Integer managerId = managerIdRaw.isBlank() ? 0 : parseNonNegativeInteger(managerIdRaw);
        if (managerId == null) {
            showResourceError("Manager ID must be numeric.");
            return null;
        }

        Integer capacity = parsePositiveInteger(hotelCapacityField.getText());
        if (capacity == null || capacity <= 0) {
            showResourceError("Capacity must be a number greater than 0.");
            return null;
        }

        return new HotelPayload(
                name,
                address,
                managerId,
                capacity
        );
    }

    private Integer parsePositiveInteger(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseNonNegativeInteger(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showResourceError(String message) {
        resourceErrorLabel.setText(safeDisplay(message, "Invalid input."));
        resourceSuccessLabel.setText("");
    }

    private void showResourceSuccess(String message) {
        resourceSuccessLabel.setText(safeDisplay(message, ""));
        resourceErrorLabel.setText("");
    }

    private void clearResourceFeedback() {
        resourceErrorLabel.setText("");
        resourceSuccessLabel.setText("");
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> refreshAll()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void navigateTo(String fxmlPath, String title) {
        if (rootPane == null || rootPane.getScene() == null) {
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Unable to open requested view.");
        }
    }

    private void redirectToRoleSelection() {
        if (rootPane == null) {
            return;
        }
        if (rootPane.getScene() == null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    redirectToRoleSelection();
                }
            });
            return;
        }
        navigateTo("/RoleSelection.fxml", "FurHope - Access Portal");
    }

    private void handleModifyReservation(ManagerReservationTicketModel ticket) {
        if (ticket == null) {
            return;
        }

        if (ticket.status() != ReservationStatus.PENDING && ticket.status() != ReservationStatus.APPROVED) {
            showAlert(Alert.AlertType.INFORMATION, "Only pending or approved reservations can be modified.");
            return;
        }

        Dialog<Pair<LocalDate, LocalDate>> dialog = new Dialog<>();
        dialog.setTitle("Modify Reservation");

        DatePicker checkInPicker = new DatePicker(ticket.checkInDate());
        DatePicker checkOutPicker = new DatePicker(ticket.checkOutDate());
        VBox content = new VBox(8,
                new Label("Check-in date"),
                checkInPicker,
                new Label("Check-out date"),
                checkOutPicker
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new Pair<>(checkInPicker.getValue(), checkOutPicker.getValue())
                : null);

        Optional<Pair<LocalDate, LocalDate>> response = dialog.showAndWait();
        if (response.isEmpty()) {
            return;
        }

        LocalDate checkIn = response.get().getKey();
        LocalDate checkOut = response.get().getValue();

        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            showAlert(Alert.AlertType.ERROR, "Invalid date range.");
            return;
        }

        UserReservationActionResult result = managerDashboardService.modifyReservationByManager(ticket.reservationId(), checkIn, checkOut);
        if (!result.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, reservationActionMessage(result.code()));
            return;
        }

        refreshReservations();
        refreshHotels();
    }

    private void handleCancelReservation(ManagerReservationTicketModel ticket) {
        if (ticket == null) {
            return;
        }

        UserReservationActionResult result = managerDashboardService.cancelReservationByManager(ticket.reservationId());
        if (!result.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, reservationActionMessage(result.code()));
            return;
        }

        refreshReservations();
        refreshHotels();
    }

    private void handleApproveReservation(ManagerReservationTicketModel ticket) {
        if (ticket == null) {
            return;
        }

        ReservationDecisionResult result = managerDashboardService.decideReservationStatus(ticket.reservationId(), ReservationStatus.APPROVED);
        if (!result.isUpdated()) {
            showAlert(Alert.AlertType.ERROR, reservationDecisionMessage(result, "approve"));
            return;
        }

        refreshReservations();
        refreshHotels();
    }

    private void handleDeclineReservation(ManagerReservationTicketModel ticket) {
        if (ticket == null) {
            return;
        }

        ReservationDecisionResult result = managerDashboardService.decideReservationStatus(ticket.reservationId(), ReservationStatus.DECLINED);
        if (!result.isUpdated()) {
            showAlert(Alert.AlertType.ERROR, reservationDecisionMessage(result, "decline"));
            return;
        }

        refreshReservations();
        refreshHotels();
    }

    private String reservationActionMessage(UserReservationActionCode code) {
        if (code == null) {
            return "Reservation operation failed.";
        }
        return switch (code) {
            case NOT_FOUND -> "Reservation was not found.";
            case FORBIDDEN -> "You are not authorized for this reservation.";
            case INVALID_DATES -> "Provided dates are invalid.";
            case CONFLICT -> "Reservation conflicts with existing bookings.";
            case INVALID_STATUS -> "Reservation status does not allow this action.";
            case FAILED -> "Reservation operation failed.";
            case UPDATED, CANCELLED -> "";
        };
    }

    private String reservationDecisionMessage(ReservationDecisionResult result, String action) {
        ReservationDecisionCode code = result == null ? null : result.code();
        if (code == null) {
            return "Could not " + action + " reservation.";
        }
        return switch (code) {
            case NOT_FOUND -> "Reservation was not found.";
            case ALREADY_PROCESSED -> "Reservation is no longer pending.";
            case INVALID_TARGET_STATUS -> "Invalid status transition requested.";
            case FAILED -> "Could not " + action + " reservation.";
            case UPDATED -> "";
        };
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Label emptyStateLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("header-subtitle");
        label.setWrapText(true);
        return label;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeDisplay(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private record HotelPayload(
            String name,
            String address,
            int managerId,
            int capacity
    ) {
    }
}
