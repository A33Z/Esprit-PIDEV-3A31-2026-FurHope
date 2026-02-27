package application.model;

import entities.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ManagerReservationTicketModel(
        int reservationId,
        String hotelName,
        String guestName,
        LocalDate reservationDate,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        BigDecimal totalPrice,
        int guestCount,
        ReservationStatus status
) {
    public ManagerReservationTicketModel withStatus(ReservationStatus updatedStatus) {
        return new ManagerReservationTicketModel(
                reservationId,
                hotelName,
                guestName,
                reservationDate,
                checkInDate,
                checkOutDate,
                nights,
                totalPrice,
                guestCount,
                updatedStatus
        );
    }
}
