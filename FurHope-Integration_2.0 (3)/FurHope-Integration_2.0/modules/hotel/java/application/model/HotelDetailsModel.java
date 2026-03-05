package application.model;

import java.util.List;

public record HotelDetailsModel(
        int hotelId,
        String name,
        double rating,
        String fullDescription,
        String priceLabel,
        String location,
        double latitude,
        double longitude,
        List<String> imageUrls,
        String weatherSummary
) {
}
