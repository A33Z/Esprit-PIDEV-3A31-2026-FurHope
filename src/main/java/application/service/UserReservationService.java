package application.service;

import entities.Reservation;
import entities.ReservationStatus;
import services.ReservationAccessService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserReservationService {

    private final ReservationAccessService reservationAccessService;
    private final HotelExplorationService hotelExplorationService;

    public UserReservationService(
            ReservationAccessService reservationAccessService,
            HotelExplorationService hotelExplorationService
    ) {
        this.reservationAccessService = reservationAccessService;
        this.hotelExplorationService = hotelExplorationService;
    }

    public List<Reservation> getCurrentUserReservations() {
        return reservationAccessService.viewCurrentUserReservations();
    }

    public boolean bookHotel(int hotelId, int animalId, LocalDate startDate, LocalDate endDate) {
        validateBookingInput(hotelId, animalId, startDate, endDate);
        Reservation reservation = new Reservation(
                0,
                animalId,
                hotelId,
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                ReservationStatus.PENDING
        );
        return reservationAccessService.addReservationForCurrentUser(reservation);
    }

    public String formatReservation(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String hotelName = hotelExplorationService.resolveHotelName(reservation.getHotelId());
        String start = reservation.getStartDate() == null
                ? "-"
                : formatter.format(reservation.getStartDate().toLocalDate());
        String end = reservation.getEndDate() == null
                ? "-"
                : formatter.format(reservation.getEndDate().toLocalDate());

        return "Reservation #" + reservation.getId()
                + " | Hotel: " + hotelName
                + " | Animal: " + reservation.getAnimalId()
                + " | " + start + " -> " + end
                + " | Status: " + reservation.getStatus().name();
    }

    private void validateBookingInput(int hotelId, int animalId, LocalDate startDate, LocalDate endDate) {
        if (hotelId <= 0) {
            throw new IllegalArgumentException("Invalid hotel selection.");
        }
        if (animalId <= 0) {
            throw new IllegalArgumentException("Animal ID must be greater than 0.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
    }
}
