package application.model;

import java.math.BigDecimal;

public record MonthlyRevenueTrendModel(
        String monthLabel,
        BigDecimal totalRevenue
) {
}
