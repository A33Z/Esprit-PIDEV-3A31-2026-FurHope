package services;

import entities.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Date;

public record ManagerReservationSnapshot(
        int reservationId,
        String hotelName,
        String guestName,
        Date reservationDate,
        Date checkInDate,
        Date checkOutDate,
        long nights,
        BigDecimal totalPrice,
        int guestCount,
        ReservationStatus status
) {
}
