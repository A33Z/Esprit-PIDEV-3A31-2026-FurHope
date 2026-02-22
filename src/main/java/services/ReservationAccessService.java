package services;

import entities.Reservation;
import entities.ReservationStatus;

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

    public boolean approveReservation(int reservationId) {
        requireManager();
        return reservationService.updateReservationStatusByManager(reservationId, ReservationStatus.APPROVED);
    }

    public boolean declineReservation(int reservationId) {
        requireManager();
        return reservationService.updateReservationStatusByManager(reservationId, ReservationStatus.DECLINED);
    }

    public List<Reservation> viewCurrentUserReservations() {
        int userId = requireUserId();
        return reservationService.getReservationsByClientId(userId);
    }

    public boolean addReservationForCurrentUser(Reservation reservation) {
        int userId = requireUserId();
        reservation.setClientId(userId);
        reservation.setStatus(ReservationStatus.PENDING);
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
        try {
            return SessionContext.requireNormalUser().getId();
        } catch (AuthorizationException e) {
            throw new AuthorizationException("Only normal users can manage reservations.");
        }
    }

    private void requireManager() {
        try {
            SessionContext.requireManager();
        } catch (AuthorizationException e) {
            throw new AuthorizationException("Only hotel managers can approve or decline reservations.");
        }
    }
}
