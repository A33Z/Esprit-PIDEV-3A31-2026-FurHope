package application.model;

public record HotelCardModel(
        int hotelId,
        String name,
        double rating,
        String shortDescription,
        String priceLabel,
        String imageUrl,
        String location,
        double latitude,
        double longitude
) {
}
