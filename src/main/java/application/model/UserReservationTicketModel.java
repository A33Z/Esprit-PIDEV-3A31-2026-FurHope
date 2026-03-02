package application.model;

import entities.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserReservationTicketModel(
        int reservationId,
        String hotelName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        BigDecimal totalPrice,
        ReservationStatus status
) {
    public boolean canModify() {
        return status == ReservationStatus.PENDING;
    }

    public boolean canCancel() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.APPROVED;
    }

    public UserReservationTicketModel withStatus(ReservationStatus updatedStatus) {
        return new UserReservationTicketModel(
                reservationId,
                hotelName,
                checkInDate,
                checkOutDate,
                nights,
                totalPrice,
                updatedStatus
        );
    }
}
