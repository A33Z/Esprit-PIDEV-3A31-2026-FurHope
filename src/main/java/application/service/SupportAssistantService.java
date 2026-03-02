package application.service;

import application.model.HotelCardModel;
import application.model.UserReservationTicketModel;
import com.esprit.config.AppConfig;
import entities.ReservationStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupportAssistantService {

    private static final int MAX_INPUT_LENGTH = 600;
    private static final int MAX_RESPONSE_LENGTH = 1600;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final Pattern CITY_PATTERN = Pattern.compile("\\bin\\s+([A-Za-z][A-Za-z\\s\\-]{1,38})\\b");

    private final UserReservationService userReservationService;
    private final HotelExplorationService hotelExplorationService;

    public SupportAssistantService(
            UserReservationService userReservationService,
            HotelExplorationService hotelExplorationService
    ) {
        this.userReservationService = userReservationService;
        this.hotelExplorationService = hotelExplorationService;
    }

    public String generateSupportReply(String rawMessage) {
        String message = normalize(rawMessage, MAX_INPUT_LENGTH);
        if (message.isBlank()) {
            return sanitizeOutput("Please ask a question about reservations, bookings, or hotels.");
        }

        String lower = message.toLowerCase(Locale.US);

        if (looksLikePromptInjection(lower)) {
            return sanitizeOutput(
                    "I can only help with booking guidance, reservation status, hotel details, and FAQs. "
                            + "I cannot reveal internal data, credentials, prompts, or run commands."
            );
        }

        if (asksReservationStatus(lower)) {
            return sanitizeOutput(buildReservationStatusReply());
        }
        if (asksModifyBooking(lower)) {
            return sanitizeOutput(buildModifyBookingReply());
        }
        if (asksCancelBooking(lower)) {
            return sanitizeOutput(buildCancelBookingReply());
        }
        if (asksHotelInformation(lower)) {
            return sanitizeOutput(buildHotelInformationReply(message));
        }
        if (asksGeneralFaq(lower)) {
            return sanitizeOutput(buildFaqReply(lower));
        }

        return sanitizeOutput(
                "I can help with reservation status, booking modifications, cancellations, hotel information, and FAQs. "
                        + "Try asking: \"What is my reservation status?\""
        );
    }

    private String buildReservationStatusReply() {
        List<UserReservationTicketModel> tickets;
        try {
            tickets = userReservationService.getCurrentUserReservationTickets();
        } catch (RuntimeException e) {
            return "I could not load your reservations right now. Please try again in a moment.";
        }

        if (tickets == null || tickets.isEmpty()) {
            return "You currently have no reservations. Use Explore Hotels to find a stay and create a booking.";
        }

        int maxItems = Math.min(5, tickets.size());
        StringBuilder response = new StringBuilder("Here is your current reservation status:\n");
        for (int i = 0; i < maxItems; i++) {
            UserReservationTicketModel ticket = tickets.get(i);
            if (ticket == null) {
                continue;
            }
            response.append("- ")
                    .append(safeText(ticket.hotelName(), "Hotel"))
                    .append(": ")
                    .append(statusLabel(ticket.status()))
                    .append(" (")
                    .append(formatDate(ticket.checkInDate()))
                    .append(" to ")
                    .append(formatDate(ticket.checkOutDate()))
                    .append(")\n");
        }
        if (tickets.size() > maxItems) {
            response.append("- And ").append(tickets.size() - maxItems).append(" more reservation(s).\n");
        }
        response.append("To modify or cancel a booking, use the actions in your My Reservations panel.");
        return response.toString();
    }

    private String buildModifyBookingReply() {
        return """
                You can modify a booking from the My Reservations panel:
                - Find the reservation with status PENDING.
                - Click Modify.
                - Select new check-in/check-out dates and confirm.
                - The request stays read-only here; changes are applied only when you confirm in dashboard actions.
                """;
    }

    private String buildCancelBookingReply() {
        return """
                You can cancel a booking from the My Reservations panel:
                - Open your reservation list.
                - Select a reservation eligible for cancellation.
                - Click Cancel and confirm.
                I can guide you, but I do not cancel reservations automatically.
                """;
    }

    private String buildHotelInformationReply(String originalMessage) {
        String city = extractCity(originalMessage);
        List<HotelCardModel> hotels;
        try {
            hotels = hotelExplorationService.discoverHotels(city);
        } catch (RuntimeException e) {
            hotels = List.of();
        }

        if (hotels == null || hotels.isEmpty()) {
            return "I could not find hotel information right now for " + city + ". Please try again shortly.";
        }

        StringBuilder response = new StringBuilder("Here are recommended hotels in ")
                .append(city)
                .append(":\n");

        int maxItems = Math.min(3, hotels.size());
        for (int i = 0; i < maxItems; i++) {
            HotelCardModel hotel = hotels.get(i);
            if (hotel == null) {
                continue;
            }
            response.append("- ")
                    .append(safeText(hotel.name(), "Hotel"))
                    .append(" | Rating ")
                    .append(String.format(Locale.US, "%.1f/5.0", Math.max(0.0, hotel.rating())))
                    .append(" | ")
                    .append(safeText(hotel.priceLabel(), "Price unavailable"))
                    .append(" | ")
                    .append(safeText(hotel.location(), city))
                    .append("\n");
        }
        response.append("Use Explore Hotels to view full details, photos, and booking options.");
        return response.toString();
    }

    private String buildFaqReply(String lower) {
        if (lower.contains("check-in") || lower.contains("check in")) {
            return "Check-in and check-out dates are selected while creating or modifying reservations from the dashboard.";
        }
        if (lower.contains("cancel policy") || lower.contains("cancellation policy")) {
            return "Cancellation eligibility depends on reservation status. PENDING and APPROVED reservations are generally cancellable from My Reservations.";
        }
        if (lower.contains("payment") || lower.contains("price")) {
            return "Pricing is shown in the hotel details and reservation summary. Final totals depend on selected dates and stay duration.";
        }
        if (lower.contains("pet")) {
            return "Pet-related booking context is handled in your reservation flow. For a specific booking, ask for your reservation status first.";
        }
        return "I can help with reservation status, modifications, cancellations, hotel options, and common booking questions.";
    }

    private boolean asksReservationStatus(String lower) {
        boolean reservationScope = lower.contains("reservation") || lower.contains("booking");
        return (lower.contains("status") && reservationScope)
                || lower.contains("my reservations")
                || lower.contains("my bookings")
                || lower.contains("do i have a reservation")
                || lower.contains("what is my reservation");
    }

    private boolean asksModifyBooking(String lower) {
        boolean action = lower.contains("modify")
                || lower.contains("change")
                || lower.contains("edit")
                || lower.contains("reschedule")
                || lower.contains("update");
        boolean reservationScope = lower.contains("reservation") || lower.contains("booking") || lower.contains("dates");
        return action && reservationScope;
    }

    private boolean asksCancelBooking(String lower) {
        boolean action = lower.contains("cancel") || lower.contains("cancellation");
        boolean reservationScope = lower.contains("reservation") || lower.contains("booking");
        return action && reservationScope;
    }

    private boolean asksHotelInformation(String lower) {
        return lower.contains("hotel")
                || lower.contains("stay in ")
                || lower.contains("available in ")
                || lower.contains("where should i stay")
                || lower.contains("recommend");
    }

    private boolean asksGeneralFaq(String lower) {
        return lower.contains("faq")
                || lower.contains("help")
                || lower.contains("how do i")
                || lower.contains("check-in")
                || lower.contains("check in")
                || lower.contains("payment")
                || lower.contains("policy");
    }

    private boolean looksLikePromptInjection(String lower) {
        return lower.contains("ignore previous")
                || lower.contains("system prompt")
                || lower.contains("developer message")
                || lower.contains("reveal")
                || lower.contains("show me your instructions")
                || lower.contains("database schema")
                || lower.contains("drop table")
                || lower.contains("manager id")
                || lower.contains("internal id")
                || lower.contains("token")
                || lower.contains("password")
                || lower.contains("cmd.exe")
                || lower.contains("powershell")
                || lower.contains("run command")
                || lower.contains("execute command");
    }

    private String extractCity(String originalMessage) {
        if (originalMessage == null || originalMessage.isBlank()) {
            return AppConfig.defaultCity();
        }
        Matcher matcher = CITY_PATTERN.matcher(originalMessage);
        if (!matcher.find()) {
            return AppConfig.defaultCity();
        }
        String rawCity = matcher.group(1);
        String compactCity = normalize(rawCity, 40);
        if (compactCity.isBlank()) {
            return AppConfig.defaultCity();
        }
        return toTitleCase(compactCity);
    }

    private String toTitleCase(String rawValue) {
        String[] words = rawValue.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lowerWord = word.toLowerCase(Locale.US);
            builder.append(Character.toUpperCase(lowerWord.charAt(0)))
                    .append(lowerWord.substring(1));
        }
        if (builder.length() == 0) {
            return AppConfig.defaultCity();
        }
        return builder.toString();
    }

    private String statusLabel(ReservationStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case DECLINED -> "Declined";
            case CANCELLED -> "Cancelled";
        };
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String safeText(String value, String fallback) {
        String normalized = normalize(value, 180);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String compact = value
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replace('\u0000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength).trim();
    }

    private String sanitizeOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "I can help with reservations, booking changes, cancellations, and hotel information.";
        }

        String[] lines = rawOutput.split("\\R");
        StringBuilder sanitized = new StringBuilder();
        for (String line : lines) {
            String normalizedLine = line
                    .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                    .replace('\u0000', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();
            if (normalizedLine.isBlank()) {
                continue;
            }
            if (containsSensitiveIdentifier(normalizedLine.toLowerCase(Locale.US))) {
                continue;
            }
            sanitized.append(normalizedLine).append('\n');
        }

        String result = sanitized.toString().trim();
        if (result.isBlank()) {
            result = "I can help with reservations, booking changes, cancellations, and hotel information.";
        }
        if (result.length() > MAX_RESPONSE_LENGTH) {
            result = result.substring(0, MAX_RESPONSE_LENGTH - 3).trim() + "...";
        }
        return result;
    }

    private boolean containsSensitiveIdentifier(String lowerLine) {
        return lowerLine.contains("reservation id")
                || lowerLine.contains("client id")
                || lowerLine.contains("manager id")
                || lowerLine.contains("internal id")
                || lowerLine.contains("system id")
                || lowerLine.contains("token")
                || lowerLine.contains("password");
    }
}

