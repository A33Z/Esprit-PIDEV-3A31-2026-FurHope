package controllers;

import application.AppContext;
import application.model.HotelDetailsModel;
import application.service.HotelExplorationService;
import application.service.UserReservationService;
import integrations.content.RealHotelImageCatalog;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HotelDetailsController {

    @FXML
    private Label hotelNameLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label weatherLabel;
    @FXML
    private Label detailsMessageLabel;
    @FXML
    private Label imageCounterLabel;
    @FXML
    private ImageView heroImageView;
    @FXML
    private HBox galleryContainer;
    @FXML
    private TextArea fullDescriptionArea;
    @FXML
    private TextField animalIdField;
    @FXML
    private TextField guestCountField;
    @FXML
    private DatePicker checkInDatePicker;
    @FXML
    private DatePicker checkOutDatePicker;
    @FXML
    private Button bookNowButton;

    private HotelExplorationService hotelExplorationService;
    private UserReservationService userReservationService;

    private HotelDetailsModel currentHotel;
    private List<String> currentImageUrls = List.of();
    private int currentImageIndex;
    private Runnable onReservationCreated = () -> {
    };

    @FXML
    public void initialize() {
        try {
            hotelExplorationService = AppContext.getInstance().hotelExplorationService();
            userReservationService = AppContext.getInstance().userReservationService();
        } catch (RuntimeException e) {
            showMessage("Could not initialize booking services.", true);
        }
        fullDescriptionArea.setEditable(false);
        fullDescriptionArea.setWrapText(true);
        heroImageView.setPreserveRatio(false);
        heroImageView.setSmooth(true);
        guestCountField.setText("1");
        configureBookingDatePickers();
    }

    public void loadHotel(int hotelId) {
        if (hotelExplorationService == null) {
            showMessage("Hotel service unavailable.", true);
            return;
        }
        showMessage("Loading hotel details...", false);
        Task<HotelDetailsModel> task = new Task<>() {
            @Override
            protected HotelDetailsModel call() {
                return hotelExplorationService.getHotelDetails(hotelId);
            }
        };
        task.setOnSucceeded(event -> {
            HotelDetailsModel details = task.getValue();
            if (details == null) {
                showMessage("Hotel details are unavailable.", true);
                return;
            }
            currentHotel = details;
            renderDetails(details);
            showMessage("", false);
        });
        task.setOnFailed(event -> showMessage("Unable to load hotel details.", true));
        runTask(task, "hotel-details-load-thread");
    }

    public void setOnReservationCreated(Runnable callback) {
        if (callback != null) {
            this.onReservationCreated = callback;
        }
    }

    @FXML
    private void handleBookNow() {
        if (userReservationService == null) {
            showMessage("Booking service unavailable.", true);
            return;
        }
        if (currentHotel == null) {
            showMessage("Select a hotel before booking.", true);
            return;
        }

        int animalId;
        try {
            animalId = parsePositiveInt(animalIdField.getText(), "Animal ID");
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
            return;
        }

        int guestCount;
        try {
            guestCount = parsePositiveIntOrDefault(guestCountField.getText(), "Number of Guests", 1);
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
            return;
        }

        LocalDate checkIn = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return userReservationService.bookHotel(
                        currentHotel.hotelId(),
                        animalId,
                        guestCount,
                        checkIn,
                        checkOut
                );
            }
        };
        bookNowButton.setDisable(true);
        task.setOnSucceeded(event -> {
            bookNowButton.setDisable(false);
            if (Boolean.TRUE.equals(task.getValue())) {
                showMessage("Reservation created with status PENDING.", false);
                onReservationCreated.run();
            } else {
                showMessage("Reservation request failed.", true);
            }
        });
        task.setOnFailed(event -> {
            bookNowButton.setDisable(false);
            Throwable exception = task.getException();
            String message = exception == null ? "Booking failed." : resolveRootMessage(exception, "Booking failed.");
            showMessage(message, true);
        });
        runTask(task, "reservation-create-thread");
    }

    @FXML
    private void handlePreviousImage() {
        if (currentImageUrls.isEmpty()) {
            return;
        }
        currentImageIndex = (currentImageIndex - 1 + currentImageUrls.size()) % currentImageUrls.size();
        updateHeroImage();
    }

    @FXML
    private void handleNextImage() {
        if (currentImageUrls.isEmpty()) {
            return;
        }
        currentImageIndex = (currentImageIndex + 1) % currentImageUrls.size();
        updateHeroImage();
    }

    @FXML
    private void handleOpenImageFullscreen() {
        if (currentImageUrls.isEmpty()) {
            return;
        }
        String imageUrl = currentImageUrls.get(currentImageIndex);
        ImageView fullscreenView = new ImageView();
        setImageWithFallback(fullscreenView, imageUrl, currentHotel == null ? 1 : currentHotel.hotelId());
        fullscreenView.setPreserveRatio(true);
        fullscreenView.setSmooth(true);
        fullscreenView.setFitWidth(1400);
        fullscreenView.setFitHeight(900);

        BorderPane root = new BorderPane(fullscreenView);
        root.setStyle("-fx-background-color: rgba(12, 18, 16, 0.98);");
        BorderPane.setAlignment(fullscreenView, Pos.CENTER);

        Scene scene = new Scene(root, 1400, 900);
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Hotel Image");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) hotelNameLabel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void renderDetails(HotelDetailsModel details) {
        hotelNameLabel.setText(details.name());
        ratingLabel.setText(String.format("Rating: %.1f / 5.0", details.rating()));
        locationLabel.setText("Location: " + details.location());
        priceLabel.setText("Price: " + details.priceLabel());
        weatherLabel.setText("Weather: " + details.weatherSummary());
        fullDescriptionArea.setText(details.fullDescription());
        renderImages(details.imageUrls());
        setDefaultDates();
    }

    private void renderImages(List<String> imageUrls) {
        galleryContainer.getChildren().clear();
        if (imageUrls == null || imageUrls.isEmpty()) {
            heroImageView.setImage(null);
            imageCounterLabel.setText("0 / 0");
            currentImageUrls = List.of();
            currentImageIndex = 0;
            return;
        }

        currentImageUrls = new ArrayList<>(imageUrls);
        currentImageIndex = 0;
        updateHeroImage();

        for (int index = 0; index < currentImageUrls.size(); index++) {
            final int imageIndex = index;
            String imageUrl = currentImageUrls.get(index);
            ImageView thumb = new ImageView();
            setImageWithFallback(thumb, imageUrl, currentHotel == null ? 1 : currentHotel.hotelId());
            thumb.setFitWidth(142);
            thumb.setFitHeight(92);
            thumb.setPreserveRatio(false);
            thumb.getStyleClass().add("gallery-image");
            thumb.setOnMouseClicked(event -> {
                currentImageIndex = imageIndex;
                updateHeroImage();
            });

            StackPane frame = new StackPane(thumb);
            frame.setPadding(new Insets(2));
            frame.setAlignment(Pos.CENTER);
            galleryContainer.getChildren().add(frame);
        }
    }

    private void updateHeroImage() {
        if (currentImageUrls.isEmpty()) {
            heroImageView.setImage(null);
            imageCounterLabel.setText("0 / 0");
            return;
        }
        String url = currentImageUrls.get(currentImageIndex);
        setImageWithFallback(heroImageView, url, currentHotel == null ? 1 : currentHotel.hotelId());
        imageCounterLabel.setText((currentImageIndex + 1) + " / " + currentImageUrls.size());
    }

    private void setImageWithFallback(ImageView targetView, String imageUrl, int hotelId) {
        String fallbackUrl = RealHotelImageCatalog.bySeed(Math.max(1, hotelId));
        Image image = imageUrl == null || imageUrl.isBlank()
                ? new Image(fallbackUrl, true)
                : new Image(imageUrl, true);
        image.errorProperty().addListener((obs, oldVal, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                targetView.setImage(new Image(fallbackUrl, true));
            }
        });
        targetView.setImage(image);
    }

    private void configureBookingDatePickers() {
        checkInDatePicker.setDayCellFactory(picker -> new DateCell() {
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
        checkOutDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(false);
                    return;
                }
                LocalDate checkIn = checkInDatePicker.getValue();
                if (checkIn == null) {
                    setDisable(date.isBefore(LocalDate.now().plusDays(1)));
                } else {
                    setDisable(!date.isAfter(checkIn));
                }
            }
        });
        checkInDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            LocalDate checkout = checkOutDatePicker.getValue();
            if (checkout == null || !checkout.isAfter(newVal)) {
                checkOutDatePicker.setValue(newVal.plusDays(1));
            }
        });
    }

    private void setDefaultDates() {
        LocalDate today = LocalDate.now();
        if (checkInDatePicker.getValue() == null || checkInDatePicker.getValue().isBefore(today)) {
            checkInDatePicker.setValue(today.plusDays(1));
        }
        if (checkOutDatePicker.getValue() == null || !checkOutDatePicker.getValue().isAfter(checkInDatePicker.getValue())) {
            checkOutDatePicker.setValue(checkInDatePicker.getValue().plusDays(1));
        }
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
    }

    private int parsePositiveIntOrDefault(String rawValue, String fieldName, int defaultValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        return parsePositiveInt(rawValue, fieldName);
    }

    private void showMessage(String message, boolean error) {
        detailsMessageLabel.setText(message);
        detailsMessageLabel.getStyleClass().removeAll("form-error", "header-subtitle");
        detailsMessageLabel.getStyleClass().add(error ? "form-error" : "header-subtitle");
    }

    private String resolveRootMessage(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message.trim();
    }

    private void runTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }
}

