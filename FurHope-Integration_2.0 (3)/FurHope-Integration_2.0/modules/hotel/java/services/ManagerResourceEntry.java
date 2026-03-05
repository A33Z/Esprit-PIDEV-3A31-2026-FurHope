package services;

import java.math.BigDecimal;
import java.time.Instant;

public record ManagerResourceEntry(
        String resourceToken,
        int hotelId,
        HotelResourceType resourceType,
        String resourceName,
        BigDecimal unitPrice,
        int capacity,
        boolean available,
        Instant updatedAt
) {
}
