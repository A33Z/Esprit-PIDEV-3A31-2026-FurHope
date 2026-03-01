package services;

import java.math.BigDecimal;
import java.util.List;

public record ManagerAnalyticsSnapshot(
        BigDecimal totalRevenue,
        int totalReservations,
        String mostBookedHotel,
        double averageOccupancyRate,
        List<MonthlyReservationTrendPoint> monthlyTrend,
        List<MonthlyRevenueTrendPoint> revenueTrend
) {
}
