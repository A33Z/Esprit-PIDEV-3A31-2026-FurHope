package services;

import java.math.BigDecimal;

public record MonthlyRevenueTrendPoint(
        String monthLabel,
        BigDecimal totalRevenue
) {
}
