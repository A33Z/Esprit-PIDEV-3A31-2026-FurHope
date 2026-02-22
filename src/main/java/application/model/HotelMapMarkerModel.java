package application.model;

public record HotelMapMarkerModel(
        int hotelId,
        String name,
        double rating,
        String preview,
        double latitude,
        double longitude
) {
}
