package services;

import entities.Reservation;
import entities.ReservationStatus;

import java.util.List;
import java.util.Map;

public class ReservationAccessService {

    private final ReservationService reservationService;

    public ReservationAccessService() {
        this.reservationService = new ReservationService();
    }

    public List<Reservation> viewAllReservationsForManager() {
        requireManager();
        return reservationService.getAllReservations();
    }

    public List<ManagerReservationSnapshot> viewReservationPageForManager(int page, int pageSize) {
        requireManager();
        return reservationService.getManagerReservationPage(page, pageSize);
    }

    public int countReservationsForManager() {
        requireManager();
        return reservationService.countAllReservations();
    }

    public ManagerAnalyticsSnapshot viewManagerAnalytics() {
        requireManager();
        return reservationService.getManagerAnalyticsSnapshot();
    }

    public ReservationDecisionResult decideReservationStatus(int reservationId, ReservationStatus targetStatus) {
        requireManager();
        return reservationService.decideReservationStatusByManager(reservationId, targetStatus);
    }

    public UserReservationActionResult modifyReservationDatesByManager(int reservationId, java.sql.Date checkInDate, java.sql.Date checkOutDate) {
        requireManager();
        return reservationService.modifyReservationDatesByManager(reservationId, checkInDate, checkOutDate);
    }

    public UserReservationActionResult cancelReservationByManager(int reservationId) {
        requireManager();
        return reservationService.cancelReservationByManager(reservationId);
    }

    public Map<Integer, HotelAvailabilitySnapshot> viewHotelAvailabilityForManager() {
        requireManager();
        return reservationService.getHotelAvailabilitySummary();
    }

    public boolean approveReservation(int reservationId) {
        requireManager();
        return reservationService.decideReservationStatusByManager(reservationId, ReservationStatus.APPROVED).isUpdated();
    }

    public boolean declineReservation(int reservationId) {
        requireManager();
        return reservationService.decideReservationStatusByManager(reservationId, ReservationStatus.DECLINED).isUpdated();
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

    public UserReservationActionResult modifyReservationDatesForCurrentUser(int reservationId, java.sql.Date checkInDate, java.sql.Date checkOutDate) {
        int userId = requireUserId();
        return reservationService.modifyReservationDatesOwnedByClient(reservationId, userId, checkInDate, checkOutDate);
    }

    public UserReservationActionResult cancelReservationForCurrentUser(int reservationId) {
        int userId = requireUserId();
        return reservationService.cancelReservationOwnedByClient(reservationId, userId);
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
