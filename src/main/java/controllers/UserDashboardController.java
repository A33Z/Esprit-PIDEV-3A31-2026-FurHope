package controllers;

import application.AppContext;
import application.model.HotelCardModel;
import application.model.HotelMapDatasetModel;
import application.model.UserReservationActionModel;
import application.model.UserReservationTicketModel;
import application.service.HotelExplorationService;
import application.service.UserReservationService;
import application.ui.UserReservationTicketCard;
import com.esprit.config.AppConfig;
import com.esprit.utils.ThemeManager;
import entities.User;
import integrations.content.RealHotelImageCatalog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.AuthorizationException;
import services.SessionContext;
import services.UserReservationActionCode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label sessionLabel;
    @FXML
    private Label dashboardMessageLabel;
    @FXML
    private TextField cityField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ComboBox<String> ratingFilterComboBox;
    @FXML
    private TilePane hotelCardContainer;
    @FXML
    private ListView<UserReservationTicketModel> myReservationListView;

    private final ObservableList<UserReservationTicketModel> myReservations = FXCollections.observableArrayList();

    private HotelExplorationService hotelExplorationService;
    private UserReservationService userReservationService;

    private Timeline autoRefreshTimeline;
    private List<HotelCardModel> loadedHotels = List.of();

    @FXML
    public void initialize() {
        User user;
        try {
            user = SessionContext.requireNormalUser();
        } catch (AuthorizationException e) {
            Platform.runLater(this::redirectToRoleSelection);
            return;
        }

        sessionLabel.setText("Logged in as: " + user.getDisplayName());

        try {
            hotelExplorationService = AppContext.getInstance().hotelExplorationService();
            userReservationService = AppContext.getInstance().userReservationService();
        } catch (RuntimeException e) {
            showMessage("Could not initialize services.", true);
            return;
        }

        cityField.setText(AppConfig.defaultCity());
        configureDashboardControls();
        configureReservationList();
        refreshReservations();
        refreshHotels();
        startAutoRefresh();
    }

    @FXML
    private void handleExploreHotels() {
        refreshHotels();
    }

    @FXML
    private void handleRefreshReservations() {
        refreshReservations();
    }

    @FXML
    private void handleOpenSupportCenter() {
        try {
            SessionContext.requireNormalUser();
        } catch (AuthorizationException e) {
            redirectToRoleSelection();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SupportCenter.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("FurHope - Support Center");
            stage.setScene(new Scene(root));
            stage.setMinWidth(920);
            stage.setMinHeight(700);
            stage.show();
        } catch (IOException e) {
            showMessage("Unable to open support center.", true);
        }
    }

    @FXML
    private void handleOpenMapView() {
        if (hotelExplorationService == null) {
            showMessage("Hotel service unavailable.", true);
            return;
        }
        String city = currentCity();
        showMessage("Loading hotels from database for map view...", false);

        Task<HotelMapDatasetModel> task = new Task<>() {
            @Override
            protected HotelMapDatasetModel call() {
                try {
                    return hotelExplorationService.loadDatabaseMapDataset(city);
                } catch (RuntimeException e) {
                    return new HotelMapDatasetModel(city, Double.NaN, Double.NaN, 0, List.of());
                }
            }
        };
        task.setOnSucceeded(event -> openMapStage(task.getValue()));
        task.setOnFailed(event -> showMessage("Unable to load hotels for map view.", true));
        runTask(task, "hotel-map-load-thread");
    }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        SessionContext.logout();
        redirectToRoleSelection();
    }

    @FXML
    public void gotowelcome(ActionEvent actionEvent) {
        stopAutoRefresh();
        SessionContext.logout();
        controllers.SessionContext.clear();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Welcome.fxml"));
            Stage stage;
            if (actionEvent != null
                    && actionEvent.getSource() instanceof Node node
                    && node.getScene() != null) {
                stage = (Stage) node.getScene().getWindow();
            } else if (rootPane != null && rootPane.getScene() != null) {
                stage = (Stage) rootPane.getScene().getWindow();
            } else {
                return;
            }

            Scene scene = new Scene(root);
            ThemeManager.applyToScene(scene);
            stage.setScene(scene);
            stage.setTitle("FurHope - Welcome");
            stage.show();
        } catch (IOException e) {
            showMessage("Unable to open welcome page.", true);
        }
    }

    @FXML
    public void gotoacceuil(ActionEvent actionEvent) {
        stopAutoRefresh();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
            Stage stage;
            if (actionEvent != null
                    && actionEvent.getSource() instanceof Node node
                    && node.getScene() != null) {
                stage = (Stage) node.getScene().getWindow();
            } else if (rootPane != null && rootPane.getScene() != null) {
                stage = (Stage) rootPane.getScene().getWindow();
            } else {
                return;
            }

            Scene scene = new Scene(root);
            ThemeManager.applyToScene(scene);
            stage.setScene(scene);
            stage.setTitle("FurHope - Home");
            stage.show();
        } catch (IOException e) {
            showMessage("Unable to open accueil page.", true);
        }
    }

    private void configureReservationList() {
        myReservationListView.setItems(myReservations);
        myReservationListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserReservationTicketModel ticket, boolean empty) {
                super.updateItem(ticket, empty);
                if (!getStyleClass().contains("reservation-ticket-cell")) {
                    getStyleClass().add("reservation-ticket-cell");
                }
                if (empty || ticket == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(new UserReservationTicketCard(
                        ticket,
                        () -> handleModifyReservation(ticket),
                        UserDashboardController.this::handleCancelReservation
                ));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    private void handleModifyReservation(UserReservationTicketModel ticket) {
        if (ticket == null || !ticket.canModify()) {
            showMessage("Only pending reservations can be modified.", true);
            return;
        }

        ModifyInput input = openModifyDialog(ticket);
        if (input == null) {
            return;
        }

        Task<UserReservationActionModel> task = new Task<>() {
            @Override
            protected UserReservationActionModel call() {
                return userReservationService.modifyReservationDates(
                        ticket.reservationId(),
                        input.checkInDate(),
                        input.checkOutDate()
                );
            }
        };
        task.setOnSucceeded(event -> handleReservationActionResult(task.getValue(), "Reservation updated and reset to PENDING."));
        task.setOnFailed(event -> showMessage("Could not modify reservation.", true));
        runTask(task, "reservation-modify-thread");
    }

    private void handleCancelReservation(UserReservationTicketModel ticket) {
        if (ticket == null || !ticket.canCancel()) {
            showMessage("This reservation cannot be cancelled.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Reservation");
        confirm.setHeaderText("Cancel this reservation?");
        confirm.setContentText(
                "Hotel: " + ticket.hotelName()
                        + "\nCheck-in: " + formatDate(ticket.checkInDate())
                        + "\nCheck-out: " + formatDate(ticket.checkOutDate())
        );
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        Task<UserReservationActionModel> task = new Task<>() {
            @Override
            protected UserReservationActionModel call() {
                return userReservationService.cancelReservation(ticket.reservationId());
            }
        };
        task.setOnSucceeded(event -> handleReservationActionResult(task.getValue(), "Reservation cancelled."));
        task.setOnFailed(event -> showMessage("Could not cancel reservation.", true));
        runTask(task, "reservation-cancel-thread");
    }

    private ModifyInput openModifyDialog(UserReservationTicketModel ticket) {
        Dialog<ModifyInput> dialog = new Dialog<>();
        dialog.setTitle("Modify Reservation");
        dialog.setHeaderText("Update check-in and check-out dates");

        DatePicker checkInPicker = new DatePicker(ticket.checkInDate());
        DatePicker checkOutPicker = new DatePicker(ticket.checkOutDate());
        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("form-error");

        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(false);
                    return;
                }
                setDisable(date.isBefore(LocalDate.now()));
            }
        });

        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(false);
                    return;
                }
                LocalDate checkIn = checkInPicker.getValue();
                if (checkIn == null) {
                    setDisable(date.isBefore(LocalDate.now().plusDays(1)));
                } else {
                    setDisable(!date.isAfter(checkIn));
                }
            }
        });

        VBox content = new VBox(8,
                new Label("Check-in Date"),
                checkInPicker,
                new Label("Check-out Date"),
                checkOutPicker,
                validationLabel
        );
        content.setPadding(new Insets(6, 0, 0, 0));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            LocalDate checkIn = checkInPicker.getValue();
            LocalDate checkOut = checkOutPicker.getValue();
            if (checkIn == null || checkOut == null) {
                validationLabel.setText("Both dates are required.");
                event.consume();
                return;
            }
            if (!checkOut.isAfter(checkIn)) {
                validationLabel.setText("Check-out must be after check-in.");
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new ModifyInput(checkInPicker.getValue(), checkOutPicker.getValue());
            }
            return null;
        });

        Optional<ModifyInput> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void handleReservationActionResult(UserReservationActionModel result, String successMessage) {
        if (result == null) {
            showMessage("Reservation action failed.", true);
            refreshReservations();
            return;
        }

        if (result.isSuccess() && result.ticket() != null) {
            upsertTicket(result.ticket());
            showMessage(successMessage, false);
            return;
        }

        if (result.code() == UserReservationActionCode.INVALID_DATES) {
            showMessage("Invalid date range.", true);
            return;
        }
        if (result.code() == UserReservationActionCode.CONFLICT) {
            showMessage("Reservation dates conflict with an existing booking.", true);
            refreshReservations();
            return;
        }
        if (result.code() == UserReservationActionCode.INVALID_STATUS) {
            showMessage("Reservation state does not allow this action.", true);
            refreshReservations();
            return;
        }
        if (result.code() == UserReservationActionCode.FORBIDDEN) {
            showMessage("Access denied for this reservation.", true);
            refreshReservations();
            return;
        }
        if (result.code() == UserReservationActionCode.NOT_FOUND) {
            showMessage("Reservation not found.", true);
            refreshReservations();
            return;
        }
        showMessage("Reservation action failed.", true);
        refreshReservations();
    }

    private void upsertTicket(UserReservationTicketModel updatedTicket) {
        for (int i = 0; i < myReservations.size(); i++) {
            if (myReservations.get(i).reservationId() == updatedTicket.reservationId()) {
                myReservations.set(i, updatedTicket);
                return;
            }
        }
        myReservations.add(0, updatedTicket);
    }

    private void refreshReservations() {
        if (userReservationService == null) {
            showMessage("Reservation service unavailable.", true);
            return;
        }
        Task<List<UserReservationTicketModel>> task = new Task<>() {
            @Override
            protected List<UserReservationTicketModel> call() {
                try {
                    return userReservationService.getCurrentUserReservationTickets();
                } catch (RuntimeException e) {
                    return List.of();
                }
            }
        };
        task.setOnSucceeded(event -> myReservations.setAll(task.getValue()));
        task.setOnFailed(event -> showMessage("Could not load reservations.", true));
        runTask(task, "reservation-refresh-thread");
    }

    private void refreshHotels() {
        if (hotelExplorationService == null) {
            showMessage("Hotel service unavailable.", true);
            return;
        }
        showLoadingState();
        Task<List<HotelCardModel>> task = new Task<>() {
            @Override
            protected List<HotelCardModel> call() {
                try {
                    return hotelExplorationService.discoverHotels(currentCity());
                } catch (RuntimeException e) {
                    return List.of();
                }
            }
        };
        task.setOnSucceeded(event -> {
            loadedHotels = task.getValue();
            applyViewOptions();
            int count = loadedHotels == null ? 0 : loadedHotels.size();
            if (count == 0) {
                showMessage("No hotels found. Try another city.", false);
            } else {
                showMessage("Loaded " + count + " hotels. Use sort/filter to refine.", false);
            }
        });
        task.setOnFailed(event -> {
            loadedHotels = List.of();
            hotelCardContainer.getChildren().clear();
            showMessage("Hotel API unavailable. Try again.", true);
        });
        runTask(task, "hotel-discovery-thread");
    }

    private void renderHotelCards(List<HotelCardModel> cards) {
        hotelCardContainer.getChildren().clear();
        if (cards == null || cards.isEmpty()) {
            Label emptyLabel = new Label("No hotels found for this city.");
            emptyLabel.getStyleClass().add("header-subtitle");
            hotelCardContainer.getChildren().add(emptyLabel);
            return;
        }

        for (HotelCardModel hotel : cards) {
            hotelCardContainer.getChildren().add(buildHotelCard(hotel));
        }
    }

    private VBox buildHotelCard(HotelCardModel hotel) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "hotel-card");
        card.setPadding(new Insets(12));
        card.setPrefWidth(510);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(496);
        imageView.setFitHeight(285);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("hotel-card-image");
        loadImageWithFallback(imageView, hotel.imageUrl(), hotel.hotelId());
        imageView.setOnMouseClicked(event -> openHotelDetails(hotel.hotelId()));

        Label nameLabel = new Label(hotel.name());
        nameLabel.getStyleClass().add("hotel-card-title");

        Label ratingLabel = new Label(String.format("Rating: %.1f / 5.0", hotel.rating()));
        ratingLabel.getStyleClass().add("hotel-card-rating");

        Label locationLabel = new Label("Location: " + hotel.location());
        locationLabel.getStyleClass().add("hotel-card-location");

        Label priceLabel = new Label("Price: " + hotel.priceLabel());
        priceLabel.getStyleClass().add("hotel-card-price");

        Button detailsButton = new Button("Details");
        detailsButton.getStyleClass().addAll("button", "primary-button");
        detailsButton.setOnAction(event -> openHotelDetails(hotel.hotelId()));

        HBox footer = new HBox(10, priceLabel, new Region(), detailsButton);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
                imageView,
                nameLabel,
                ratingLabel,
                locationLabel,
                footer
        );
        return card;
    }

    private void configureDashboardControls() {
        sortComboBox.getItems().setAll("Top Rated", "Lowest Price", "Highest Price", "Name A-Z");
        sortComboBox.setValue("Top Rated");

        ratingFilterComboBox.getItems().setAll("All Ratings", "4.5+", "4.0+", "3.5+");
        ratingFilterComboBox.setValue("All Ratings");

        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyViewOptions());
        ratingFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyViewOptions());
        cityField.setOnAction(event -> refreshHotels());
    }

    private void applyViewOptions() {
        List<HotelCardModel> hotels = loadedHotels == null ? List.of() : loadedHotels;
        double minRating = selectedMinRating();

        List<HotelCardModel> filtered = hotels.stream()
                .filter(hotel -> hotel.rating() >= minRating)
                .collect(Collectors.toList());

        Comparator<HotelCardModel> comparator = Comparator.comparingDouble(HotelCardModel::rating).reversed();
        String sort = sortComboBox == null ? "Top Rated" : sortComboBox.getValue();
        if ("Lowest Price".equals(sort)) {
            comparator = Comparator.comparingDouble(h -> parsePriceValue(h.priceLabel()));
        } else if ("Highest Price".equals(sort)) {
            comparator = Comparator.comparingDouble((HotelCardModel h) -> parsePriceValue(h.priceLabel())).reversed();
        } else if ("Name A-Z".equals(sort)) {
            comparator = Comparator.comparing(HotelCardModel::name, String.CASE_INSENSITIVE_ORDER);
        }
        filtered.sort(comparator);
        renderHotelCards(filtered);
    }

    private double selectedMinRating() {
        String filter = ratingFilterComboBox == null ? "All Ratings" : ratingFilterComboBox.getValue();
        if ("4.5+".equals(filter)) {
            return 4.5;
        }
        if ("4.0+".equals(filter)) {
            return 4.0;
        }
        if ("3.5+".equals(filter)) {
            return 3.5;
        }
        return 0.0;
    }

    private double parsePriceValue(String priceLabel) {
        if (priceLabel == null || priceLabel.isBlank()) {
            return Double.MAX_VALUE;
        }
        String numeric = priceLabel.replaceAll("[^0-9.]", " ").trim();
        if (numeric.isBlank()) {
            return Double.MAX_VALUE;
        }
        String firstToken = numeric.split("\\s+")[0];
        try {
            return Double.parseDouble(firstToken);
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }

    private void loadImageWithFallback(ImageView imageView, String imageUrl, int hotelId) {
        String fallbackUrl = RealHotelImageCatalog.bySeed(Math.max(1, hotelId));
        Image image = imageUrl == null || imageUrl.isBlank()
                ? new Image(fallbackUrl, true)
                : new Image(imageUrl, true);
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                imageView.setImage(new Image(fallbackUrl, true));
            }
        });
        imageView.setImage(image);
    }

    private void openHotelDetails(int hotelId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HotelDetailsView.fxml"));
            Parent root = loader.load();

            HotelDetailsController controller = loader.getController();
            controller.loadHotel(hotelId);
            controller.setOnReservationCreated(this::refreshReservations);

            Stage stage = new Stage();
            stage.setTitle("FurHope - Hotel Details");
            stage.setScene(new Scene(root));
            stage.setMinWidth(920);
            stage.setMinHeight(720);
            stage.show();
        } catch (IOException e) {
            showMessage("Unable to open hotel details.", true);
        }
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(20), event -> refreshReservations())
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    private void showLoadingState() {
        hotelCardContainer.getChildren().clear();
        Label loading = new Label("Loading hotels...");
        loading.getStyleClass().add("header-subtitle");
        hotelCardContainer.getChildren().add(loading);
    }

    private void runTask(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void openMapStage(HotelMapDatasetModel dataset) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HotelMapView.fxml"));
            Parent root = loader.load();

            HotelMapController controller = loader.getController();
            controller.initializeMap(dataset);

            Stage stage = new Stage();
            stage.setTitle("FurHope - Hotel Map");
            stage.setScene(new Scene(root));
            stage.setMinWidth(980);
            stage.setMinHeight(700);
            stage.show();

            int markerCount = dataset == null || dataset.markers() == null ? 0 : dataset.markers().size();
            showMessage("Map ready: " + markerCount + " hotel pin" + (markerCount == 1 ? "" : "s") + " loaded.", false);
        } catch (IOException e) {
            showMessage("Unable to open map view.", true);
        }
    }

    private String currentCity() {
        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            return AppConfig.defaultCity();
        }
        return city.trim();
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private void showMessage(String message, boolean error) {
        dashboardMessageLabel.setText(message);
        dashboardMessageLabel.getStyleClass().removeAll("form-error", "header-subtitle");
        dashboardMessageLabel.getStyleClass().add(error ? "form-error" : "header-subtitle");
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
            showMessage("Unable to return to access portal.", true);
        }
    }

    private record ModifyInput(LocalDate checkInDate, LocalDate checkOutDate) {
    }
}


