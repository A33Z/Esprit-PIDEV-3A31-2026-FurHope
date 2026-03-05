package application.service;

import application.model.UserReservationActionModel;
import application.model.UserReservationTicketModel;
import entities.Reservation;
import entities.ReservationStatus;
import services.ReservationAccessService;
import services.ReservationConflictException;
import services.UserReservationActionCode;
import services.UserReservationActionResult;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    public List<UserReservationTicketModel> getCurrentUserReservationTickets() {
        return reservationAccessService.viewCurrentUserReservations().stream()
                .map(this::toTicketModel)
                .toList();
    }

    public boolean bookHotel(int hotelId, int animalId, LocalDate startDate, LocalDate endDate) {
        return bookHotel(hotelId, animalId, 1, startDate, endDate);
    }

    public boolean bookHotel(int hotelId, int animalId, int guestCount, LocalDate startDate, LocalDate endDate) {
        validateBookingInput(hotelId, animalId, guestCount, startDate, endDate);

        BigDecimal nightlyRate = hotelExplorationService.resolveNightlyRate(hotelId);
        Reservation reservation = new Reservation(
                0,
                animalId,
                hotelId,
                Date.valueOf(LocalDate.now()),
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                guestCount,
                nightlyRate,
                BigDecimal.ZERO,
                ReservationStatus.PENDING
        );
        return reservationAccessService.addReservationForCurrentUser(reservation);
    }

    public UserReservationActionModel modifyReservationDates(int reservationId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (reservationId <= 0) {
            return new UserReservationActionModel(UserReservationActionCode.NOT_FOUND, null);
        }
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            return new UserReservationActionModel(UserReservationActionCode.INVALID_DATES, null);
        }

        try {
            UserReservationActionResult result = reservationAccessService.modifyReservationDatesForCurrentUser(
                    reservationId,
                    Date.valueOf(checkInDate),
                    Date.valueOf(checkOutDate)
            );
            return toActionModel(result);
        } catch (ReservationConflictException e) {
            return new UserReservationActionModel(UserReservationActionCode.CONFLICT, null);
        }
    }

    public UserReservationActionModel cancelReservation(int reservationId) {
        if (reservationId <= 0) {
            return new UserReservationActionModel(UserReservationActionCode.NOT_FOUND, null);
        }
        UserReservationActionResult result = reservationAccessService.cancelReservationForCurrentUser(reservationId);
        return toActionModel(result);
    }

    private UserReservationActionModel toActionModel(UserReservationActionResult result) {
        if (result == null) {
            return new UserReservationActionModel(UserReservationActionCode.FAILED, null);
        }
        return new UserReservationActionModel(result.code(), toTicketModel(result.reservation()));
    }

    private UserReservationTicketModel toTicketModel(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        long nights = calculateNights(reservation.getStartDate(), reservation.getEndDate());
        BigDecimal totalPrice = reservation.getTotalPrice();
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal nightlyRate = reservation.getNightlyRate() == null
                    ? hotelExplorationService.resolveNightlyRate(reservation.getHotelId())
                    : reservation.getNightlyRate();
            totalPrice = nightlyRate.multiply(BigDecimal.valueOf(nights));
        }

        return new UserReservationTicketModel(
                reservation.getId(),
                hotelExplorationService.resolveHotelName(reservation.getHotelId()),
                reservation.getStartDate() == null ? null : reservation.getStartDate().toLocalDate(),
                reservation.getEndDate() == null ? null : reservation.getEndDate().toLocalDate(),
                nights,
                totalPrice,
                reservation.getStatus()
        );
    }

    private long calculateNights(Date checkInDate, Date checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(checkInDate.toLocalDate(), checkOutDate.toLocalDate());
        return Math.max(0, nights);
    }

    private void validateBookingInput(int hotelId, int animalId, int guestCount, LocalDate startDate, LocalDate endDate) {
        if (hotelId <= 0) {
            throw new IllegalArgumentException("Invalid hotel selection.");
        }
        if (animalId <= 0) {
            throw new IllegalArgumentException("Animal ID must be greater than 0.");
        }
        if (guestCount <= 0) {
            throw new IllegalArgumentException("Guest count must be greater than 0.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
    }
}
