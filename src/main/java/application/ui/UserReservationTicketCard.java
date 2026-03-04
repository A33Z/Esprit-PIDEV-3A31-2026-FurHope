package application.ui;

import application.model.UserReservationTicketModel;
import entities.ReservationStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class UserReservationTicketCard extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final Button modifyButton = new Button("Modify");
    private final Button cancelButton = new Button("Cancel");

    public UserReservationTicketCard(
            UserReservationTicketModel ticket,
            Runnable modifyHandler,
            Consumer<UserReservationTicketModel> cancelHandler
    ) {
        getStyleClass().add("user-reservation-ticket");
        setSpacing(10);
        setPadding(new Insets(12, 14, 12, 14));

        Label hotelNameLabel = new Label(normalize(ticket.hotelName(), "Unknown Hotel"));
        hotelNameLabel.getStyleClass().add("user-ticket-title");

        Label statusBadge = new Label(ticket.status().name());
        statusBadge.getStyleClass().addAll("ticket-status-badge", userStatusStyleClass(ticket.status()));
        applyTicketVisualState(ticket.status());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(10, hotelNameLabel, headerSpacer, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("ticket-divider");
        divider.setPrefHeight(1.4);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.getStyleClass().add("user-ticket-grid");
        grid.add(infoBlock("Check-in Date", formatDate(ticket.checkInDate())), 0, 0);
        grid.add(infoBlock("Check-out Date", formatDate(ticket.checkOutDate())), 1, 0);
        grid.add(infoBlock("Nights", String.valueOf(Math.max(0, ticket.nights()))), 0, 1);
        grid.add(infoBlock("Total Price", formatMoney(ticket.totalPrice())), 1, 1);

        modifyButton.getStyleClass().addAll("button", "secondary-button");
        cancelButton.getStyleClass().addAll("button", "danger-button");
        modifyButton.setOnAction(event -> {
            if (modifyHandler != null) {
                modifyHandler.run();
            }
        });
        cancelButton.setOnAction(event -> {
            if (cancelHandler != null) {
                cancelHandler.accept(ticket);
            }
        });

        applyActionRules(ticket.status());

        HBox actions = new HBox(8, modifyButton, cancelButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, divider, grid, actions);
    }

    private VBox infoBlock(String label, String value) {
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("user-ticket-label");

        Label fieldValue = new Label(value);
        fieldValue.getStyleClass().add("user-ticket-value");

        VBox block = new VBox(2, fieldLabel, fieldValue);
        block.getStyleClass().add("user-ticket-block");
        return block;
    }

    private void applyActionRules(ReservationStatus status) {
        if (status == ReservationStatus.PENDING) {
            modifyButton.setDisable(false);
            cancelButton.setDisable(false);
            return;
        }
        if (status == ReservationStatus.APPROVED) {
            modifyButton.setDisable(true);
            cancelButton.setDisable(false);
            return;
        }
        modifyButton.setDisable(true);
        cancelButton.setDisable(true);
    }

    private void applyTicketVisualState(ReservationStatus status) {
        getStyleClass().removeAll("user-ticket-cancelled", "user-ticket-declined");
        if (status == ReservationStatus.CANCELLED) {
            getStyleClass().add("user-ticket-cancelled");
            return;
        }
        if (status == ReservationStatus.DECLINED) {
            getStyleClass().add("user-ticket-declined");
        }
    }

    private String userStatusStyleClass(ReservationStatus status) {
        if (status == ReservationStatus.APPROVED) {
            return "ticket-status-approved";
        }
        if (status == ReservationStatus.DECLINED) {
            return "ticket-status-declined";
        }
        if (status == ReservationStatus.CANCELLED) {
            return "ticket-status-cancelled";
        }
        return "ticket-status-pending";
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return CURRENCY_FORMAT.format(0);
        }
        return CURRENCY_FORMAT.format(value);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
