package application.model;

public record HotelMapMarkerModel(
        int hotelId,
        String name,
        double rating,
        String pricePerNight,
        double latitude,
        double longitude
) {
}
