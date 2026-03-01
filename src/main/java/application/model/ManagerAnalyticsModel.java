package application.model;

import java.math.BigDecimal;
import java.util.List;

public record ManagerAnalyticsModel(
        BigDecimal totalRevenue,
        int totalReservations,
        String mostBookedHotel,
        double averageOccupancyRate,
        List<MonthlyReservationTrendModel> monthlyTrend,
        List<MonthlyRevenueTrendModel> revenueTrend
) {
}
