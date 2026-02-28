package application.model;

import java.math.BigDecimal;

public record ManagerResourceModel(
        String resourceToken,
        int hotelId,
        String hotelName,
        String resourceType,
        String resourceName,
        BigDecimal unitPrice,
        int capacity,
        boolean available
) {
}
