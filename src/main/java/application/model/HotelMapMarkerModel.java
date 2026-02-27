package application.model;

public record HotelMapMarkerModel(
        int hotelId,
        String name,
        String address,
        int capacity,
        String shortDescription,
        double latitude,
        double longitude
) {
}
