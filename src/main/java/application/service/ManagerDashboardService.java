package application.service;

import entities.Hotel;
import entities.Reservation;
import services.HotelAccessService;
import services.ReservationAccessService;

import java.util.List;

public class ManagerDashboardService {

    private final HotelAccessService hotelAccessService;
    private final ReservationAccessService reservationAccessService;

    public ManagerDashboardService(HotelAccessService hotelAccessService, ReservationAccessService reservationAccessService) {
        this.hotelAccessService = hotelAccessService;
        this.reservationAccessService = reservationAccessService;
    }

    public List<Hotel> getHotels() {
        return hotelAccessService.viewAllHotels();
    }

    public boolean addHotel(Hotel hotel) {
        return hotelAccessService.addHotel(hotel);
    }

    public boolean updateHotel(Hotel hotel) {
        return hotelAccessService.updateHotel(hotel);
    }

    public boolean deleteHotel(int hotelId) {
        return hotelAccessService.deleteHotel(hotelId);
    }

    public List<Reservation> getReservationsForManager() {
        return reservationAccessService.viewAllReservationsForManager();
    }

    public boolean approveReservation(int reservationId) {
        return reservationAccessService.approveReservation(reservationId);
    }

    public boolean declineReservation(int reservationId) {
        return reservationAccessService.declineReservation(reservationId);
    }
}
