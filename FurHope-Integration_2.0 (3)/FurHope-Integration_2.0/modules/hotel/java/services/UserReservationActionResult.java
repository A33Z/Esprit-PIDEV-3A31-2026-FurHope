package services;

import entities.Reservation;

public record UserReservationActionResult(
        UserReservationActionCode code,
        Reservation reservation
) {

    public static UserReservationActionResult updated(Reservation reservation) {
        return new UserReservationActionResult(UserReservationActionCode.UPDATED, reservation);
    }

    public static UserReservationActionResult cancelled(Reservation reservation) {
        return new UserReservationActionResult(UserReservationActionCode.CANCELLED, reservation);
    }

    public static UserReservationActionResult notFound() {
        return new UserReservationActionResult(UserReservationActionCode.NOT_FOUND, null);
    }

    public static UserReservationActionResult forbidden() {
        return new UserReservationActionResult(UserReservationActionCode.FORBIDDEN, null);
    }

    public static UserReservationActionResult invalidDates() {
        return new UserReservationActionResult(UserReservationActionCode.INVALID_DATES, null);
    }

    public static UserReservationActionResult conflict() {
        return new UserReservationActionResult(UserReservationActionCode.CONFLICT, null);
    }

    public static UserReservationActionResult invalidStatus(Reservation reservation) {
        return new UserReservationActionResult(UserReservationActionCode.INVALID_STATUS, reservation);
    }

    public static UserReservationActionResult failed() {
        return new UserReservationActionResult(UserReservationActionCode.FAILED, null);
    }

    public boolean isSuccess() {
        return code == UserReservationActionCode.UPDATED || code == UserReservationActionCode.CANCELLED;
    }
}
