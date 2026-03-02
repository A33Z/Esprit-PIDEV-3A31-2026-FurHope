package application.ui;

import application.model.ManagerHotelInfoModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class HotelInfoCard extends VBox {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    public HotelInfoCard(ManagerHotelInfoModel hotel, Runnable onEditRequested) {
        getStyleClass().add("manager-hotel-card");
        setSpacing(10);
        setPadding(new Insets(12, 12, 12, 12));
        setPrefWidth(360);

        Label titleLabel = new Label(normalize(hotel.hotelName(), "Unknown Hotel"));
        titleLabel.getStyleClass().add("manager-hotel-title");

        Label statusChip = new Label(normalize(hotel.availabilityStatus(), "Unknown"));
        statusChip.getStyleClass().addAll("manager-hotel-status-chip", availabilityStatusStyle(hotel.availabilityStatus()));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLabel, headerSpacer, statusChip);
        header.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("ticket-divider");
        divider.setPrefHeight(1.1);

        StackPane thumbnailPane = buildThumbnail(hotel.thumbnailUrl(), hotel.hotelName());
        VBox detailPane = buildDetailPane(hotel);
        HBox content = new HBox(12, thumbnailPane, detailPane);
        content.setAlignment(Pos.TOP_LEFT);

        if (onEditRequested != null) {
            Button editButton = new Button("Edit in Form");
            editButton.getStyleClass().addAll("button", "secondary-button", "manager-hotel-edit-button");
            editButton.setOnAction(event -> onEditRequested.run());

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            HBox footer = new HBox(8, footerSpacer, editButton);
            footer.setAlignment(Pos.CENTER_RIGHT);
            getChildren().addAll(header, divider, content, footer);
            return;
        }

        getChildren().addAll(header, divider, content);
    }

    private StackPane buildThumbnail(String thumbnailUrl, String hotelName) {
        StackPane wrapper = new StackPane();
        wrapper.getStyleClass().add("manager-hotel-thumbnail-wrap");
        wrapper.setPrefSize(92, 72);
        wrapper.setMinSize(92, 72);
        wrapper.setMaxSize(92, 72);

        Label fallback = new Label(initials(hotelName));
        fallback.getStyleClass().add("manager-hotel-thumbnail-fallback");

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(92);
        imageView.setFitHeight(72);
        imageView.getStyleClass().add("manager-hotel-thumbnail-image");

        Rectangle clip = new Rectangle(92, 72);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        imageView.setClip(clip);

        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            Image image = new Image(thumbnailUrl, true);
            imageView.setImage(image);
            image.errorProperty().addListener((obs, oldVal, hasError) -> imageView.setVisible(!hasError));
        } else {
            imageView.setVisible(false);
        }

        wrapper.getChildren().addAll(fallback, imageView);
        return wrapper;
    }

    private VBox buildDetailPane(ManagerHotelInfoModel hotel) {
        Label locationLine = infoLine("Location", normalize(hotel.location(), "N/A"));
        Label ratingLine = infoLine("Rating", String.format(Locale.US, "%.1f", hotel.starRating()));
        Label totalRoomsLine = infoLine("Rooms", String.valueOf(Math.max(0, hotel.totalRooms())));
        Label priceLine = infoLine("Price", formatPrice(hotel.referencePrice()));
        priceLine.getStyleClass().add("manager-hotel-price");

        VBox details = new VBox(5, locationLine, ratingLine, totalRoomsLine, priceLine);
        details.getStyleClass().add("manager-hotel-details");
        HBox.setHgrow(details, Priority.ALWAYS);
        return details;
    }

    private Label infoLine(String label, String value) {
        Label line = new Label(label + ": " + value);
        line.getStyleClass().add("manager-hotel-meta");
        return line;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "N/A";
        }
        return CURRENCY_FORMAT.format(price);
    }

    private String initials(String hotelName) {
        String normalized = normalize(hotelName, "H");
        String[] parts = normalized.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private String availabilityStatusStyle(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toLowerCase();
        if ("open".equals(status) || "available".equals(status)) {
            return "hotel-status-open";
        }
        if ("limited".equals(status)) {
            return "hotel-status-limited";
        }
        if ("nearly full".equals(status)) {
            return "hotel-status-full";
        }
        return "hotel-status-unknown";
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
