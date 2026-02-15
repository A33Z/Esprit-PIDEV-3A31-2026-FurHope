package services;

import entities.Reservation;
import entities.Role;
import security.AuthorizationException;
import security.SessionContext;

import java.util.List;

public class ReservationAccessService {

    private final ReservationService reservationService;

    public ReservationAccessService() {
        this.reservationService = new ReservationService();
    }

    public List<Reservation> viewAllReservationsForManager() {
        requireManager();
        return reservationService.getAllReservations();
    }

    public List<Reservation> viewCurrentUserReservations() {
        int userId = requireUserId();
        return reservationService.getReservationsByClientId(userId);
    }

    public boolean addReservationForCurrentUser(Reservation reservation) {
        int userId = requireUserId();
        reservation.setClientId(userId);
        return reservationService.addReservation(reservation);
    }

    public boolean updateReservationForCurrentUser(Reservation reservation) {
        int userId = requireUserId();
        reservation.setClientId(userId);
        return reservationService.updateReservationOwnedByClient(reservation, userId);
    }

    public boolean deleteReservationForCurrentUser(int reservationId) {
        int userId = requireUserId();
        return reservationService.deleteReservationOwnedByClient(reservationId, userId);
    }

    private int requireUserId() {
        if (SessionContext.requireUser().getRole() != Role.USER) {
            throw new AuthorizationException("Only normal users can manage reservations.");
        }
        return SessionContext.requireUser().getId();
    }

    private void requireManager() {
        if (SessionContext.requireUser().getRole() != Role.HOTEL_MANAGER) {
            throw new AuthorizationException("Only hotel managers can view all reservations.");
        }
    }
}
