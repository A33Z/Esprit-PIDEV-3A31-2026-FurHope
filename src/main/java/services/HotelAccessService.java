package services;

import entities.Hotel;
import entities.Role;
import security.AuthorizationException;
import security.SessionContext;

import java.util.List;

public class HotelAccessService {

    private final HotelService hotelService;

    public HotelAccessService() {
        this.hotelService = new HotelService();
    }

    public List<Hotel> viewAllHotels() {
        return hotelService.getAllHotels();
    }

    public boolean addHotel(Hotel hotel) {
        requireManager();
        return hotelService.addHotel(hotel);
    }

    public boolean updateHotel(Hotel hotel) {
        requireManager();
        return hotelService.updateHotel(hotel);
    }

    public boolean deleteHotel(int hotelId) {
        requireManager();
        return hotelService.deleteHotel(hotelId);
    }

    private void requireManager() {
        if (SessionContext.requireUser().getRole() != Role.HOTEL_MANAGER) {
            throw new AuthorizationException("Only hotel managers can modify hotels.");
        }
    }
}
