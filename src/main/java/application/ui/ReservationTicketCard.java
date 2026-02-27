package application.ui;

import application.model.ManagerReservationTicketModel;
import entities.ReservationStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReservationTicketCard extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final Label statusBadge = new Label();
    private final HBox actionRow = new HBox(8);
    private final HBox decisionActionGroup = new HBox(8);

    public ReservationTicketCard(
            ManagerReservationTicketModel ticket,
            Runnable modifyAction,
            Runnable cancelAction,
            Runnable approveAction,
            Runnable declineAction
    ) {
        getStyleClass().add("manager-reservation-ticket");
        setSpacing(10);
        setPadding(new Insets(13, 14, 13, 14));

        Label hotelNameLabel = new Label(normalize(ticket.hotelName(), "Unknown Hotel"));
        hotelNameLabel.getStyleClass().add("manager-ticket-title");

        statusBadge.getStyleClass().add("ticket-status-badge");
        applyStatusVisualState(ticket.status());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(12, hotelNameLabel, headerSpacer, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("manager-ticket-header");

        Region divider = new Region();
        divider.getStyleClass().add("ticket-divider");
        divider.setPrefHeight(1.2);

        FlowPane metadataRow = new FlowPane();
        metadataRow.getStyleClass().add("manager-ticket-meta-row");
        metadataRow.setHgap(8);
        metadataRow.setVgap(8);
        metadataRow.getChildren().addAll(
                infoChip("Reservation Date", formatDate(ticket.reservationDate())),
                infoChip("Check-in", formatDate(ticket.checkInDate())),
                infoChip("Check-out", formatDate(ticket.checkOutDate())),
                infoChip("Guests", String.valueOf(Math.max(1, ticket.guestCount()))),
                infoChip("Nights", String.valueOf(Math.max(0, ticket.nights()))),
                infoChip("Total Price", formatPrice(ticket.totalPrice()))
        );

        Button modifyButton = actionButton("Modify", "secondary-button", modifyAction);
        Button cancelButton = actionButton("Cancel", "ghost-button", cancelAction);
        Button approveButton = actionButton("Approve", "primary-button", approveAction);
        Button declineButton = actionButton("Decline", "danger-button", declineAction);

        HBox manageActionGroup = new HBox(8, modifyButton, cancelButton);
        decisionActionGroup.getChildren().setAll(approveButton, declineButton);

        Label guestLabel = new Label("Guest: " + normalize(ticket.guestName(), "Registered Guest"));
        guestLabel.getStyleClass().add("manager-ticket-subtitle");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        actionRow.getStyleClass().add("manager-ticket-actions");
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getChildren().setAll(guestLabel, actionSpacer, manageActionGroup, decisionActionGroup);
        updateActionVisibility(ticket.status());

        getChildren().addAll(header, divider, metadataRow, actionRow);
    }

    private HBox infoChip(String label, String value) {
        Label chipLabel = new Label(label);
        chipLabel.getStyleClass().add("manager-ticket-chip-label");

        Label chipValue = new Label(value);
        chipValue.getStyleClass().add("manager-ticket-chip-value");

        HBox chip = new HBox(5, chipLabel, chipValue);
        chip.getStyleClass().add("manager-ticket-meta-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private Button actionButton(String label, String style, Runnable action) {
        Button button = new Button(label);
        button.getStyleClass().addAll("button", style, "manager-ticket-action-button");
        button.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }

    private void updateActionVisibility(ReservationStatus status) {
        ReservationStatus normalizedStatus = status == null ? ReservationStatus.PENDING : status;
        if (normalizedStatus == ReservationStatus.PENDING) {
            decisionActionGroup.setManaged(true);
            decisionActionGroup.setVisible(true);
            actionRow.setManaged(true);
            actionRow.setVisible(true);
            return;
        }
        if (normalizedStatus == ReservationStatus.APPROVED) {
            decisionActionGroup.setManaged(false);
            decisionActionGroup.setVisible(false);
            actionRow.setManaged(true);
            actionRow.setVisible(true);
            return;
        }
        actionRow.setManaged(false);
        actionRow.setVisible(false);
    }

    private void applyStatusVisualState(ReservationStatus status) {
        ReservationStatus normalizedStatus = status == null ? ReservationStatus.PENDING : status;
        statusBadge.setText(toDisplayStatus(normalizedStatus));

        getStyleClass().removeAll("manager-ticket-pending", "manager-ticket-approved", "manager-ticket-declined", "manager-ticket-cancelled");
        statusBadge.getStyleClass().removeAll("ticket-status-pending", "ticket-status-approved", "ticket-status-declined", "ticket-status-cancelled");

        if (normalizedStatus == ReservationStatus.APPROVED) {
            getStyleClass().add("manager-ticket-approved");
            statusBadge.getStyleClass().add("ticket-status-approved");
            return;
        }
        if (normalizedStatus == ReservationStatus.DECLINED) {
            getStyleClass().add("manager-ticket-declined");
            statusBadge.getStyleClass().add("ticket-status-declined");
            return;
        }
        if (normalizedStatus == ReservationStatus.CANCELLED) {
            getStyleClass().add("manager-ticket-cancelled");
            statusBadge.getStyleClass().add("ticket-status-cancelled");
            return;
        }
        getStyleClass().add("manager-ticket-pending");
        statusBadge.getStyleClass().add("ticket-status-pending");
    }

    private String toDisplayStatus(ReservationStatus status) {
        if (status == null) {
            return "Pending";
        }
        return switch (status) {
            case APPROVED -> "Approved";
            case DECLINED -> "Declined";
            case CANCELLED -> "Cancelled";
            case PENDING -> "Pending";
        };
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String formatPrice(BigDecimal value) {
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
