package application.model;

import java.math.BigDecimal;

public record ManagerHotelInfoModel(
        int hotelId,
        String hotelName,
        String location,
        double starRating,
        int totalRooms,
        int availableRooms,
        String availabilityStatus,
        BigDecimal referencePrice,
        String thumbnailUrl
) {
}
