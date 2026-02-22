package controllers;

import application.AppContext;
import application.model.HotelCardModel;
import application.service.HotelExplorationService;
import application.service.UserReservationService;
import config.AppConfig;
import entities.Reservation;
import entities.ReservationStatus;
import entities.User;
import integrations.content.RealHotelImageCatalog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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
    private ListView<Reservation> myReservationListView;

    private final ObservableList<Reservation> myReservations = FXCollections.observableArrayList();

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

        sessionLabel.setText("Logged in as: " + user.getDisplayName() + " (ID " + user.getId() + ")");

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
    private void handleOpenMapView() {
        if (loadedHotels.isEmpty()) {
            showMessage("Load hotels before opening the map.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HotelMapView.fxml"));
            Parent root = loader.load();

            HotelMapController controller = loader.getController();
            controller.initializeMap(
                    currentCity(),
                    hotelExplorationService.toMapMarkers(loadedHotels)
            );

            Stage stage = new Stage();
            stage.setTitle("FurHope - Hotel Map");
            stage.setScene(new Scene(root));
            stage.setMinWidth(980);
            stage.setMinHeight(700);
            stage.show();
        } catch (IOException e) {
            showMessage("Unable to open map view.", true);
        }
    }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        SessionContext.logout();
        redirectToRoleSelection();
    }

    private void configureReservationList() {
        myReservationListView.setItems(myReservations);
        myReservationListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reservation reservation, boolean empty) {
                super.updateItem(reservation, empty);
                if (!getStyleClass().contains("reservation-ticket-cell")) {
                    getStyleClass().add("reservation-ticket-cell");
                }
                if (empty || reservation == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setGraphic(buildReservationTicket(reservation));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    private VBox buildReservationTicket(Reservation reservation) {
        String hotelName = hotelExplorationService.resolveHotelName(reservation.getHotelId());
        String start = formatDate(reservation.getStartDate() == null ? null : reservation.getStartDate().toLocalDate());
        String end = formatDate(reservation.getEndDate() == null ? null : reservation.getEndDate().toLocalDate());

        Label titleLabel = new Label(hotelName);
        titleLabel.getStyleClass().add("ticket-title");

        Label reservationCodeLabel = new Label("Ticket #" + reservation.getId());
        reservationCodeLabel.getStyleClass().add("ticket-code");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(reservation.getStatus().name());
        statusBadge.getStyleClass().addAll("ticket-status-badge", statusStyleClass(reservation.getStatus()));

        HBox header = new HBox(8, titleLabel, spacer, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ticket-header");

        Label dateLine = new Label(start + " -> " + end);
        dateLine.getStyleClass().add("ticket-meta");

        Label infoLine = new Label("Animal ID: " + reservation.getAnimalId() + " | Hotel ID: " + reservation.getHotelId());
        infoLine.getStyleClass().add("ticket-meta");

        Region divider = new Region();
        divider.getStyleClass().add("ticket-divider");
        divider.setPrefHeight(1.5);

        VBox card = new VBox(7, header, reservationCodeLabel, divider, dateLine, infoLine);
        card.getStyleClass().add("reservation-ticket");
        card.setPadding(new Insets(12, 14, 12, 14));
        return card;
    }

    private String statusStyleClass(ReservationStatus status) {
        if (status == ReservationStatus.APPROVED) {
            return "ticket-status-approved";
        }
        if (status == ReservationStatus.DECLINED) {
            return "ticket-status-declined";
        }
        return "ticket-status-pending";
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private void refreshReservations() {
        if (userReservationService == null) {
            showMessage("Reservation service unavailable.", true);
            return;
        }
        Task<List<Reservation>> task = new Task<>() {
            @Override
            protected List<Reservation> call() {
                try {
                    return userReservationService.getCurrentUserReservations();
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

    private String currentCity() {
        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            return AppConfig.defaultCity();
        }
        return city.trim();
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
}
