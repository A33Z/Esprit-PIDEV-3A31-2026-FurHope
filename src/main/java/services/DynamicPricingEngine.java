package services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DynamicPricingEngine {

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal OCCUPANCY_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal SHORT_NOTICE_MULTIPLIER = new BigDecimal("1.10");

    public BigDecimal calculateFinalTotal(
            BigDecimal baseNightlyRate,
            Date checkInDate,
            Date checkOutDate,
            boolean occupancyAboveThreshold
    ) {
        if (baseNightlyRate == null || baseNightlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base nightly rate must be greater than 0.");
        }
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Reservation dates are required.");
        }

        LocalDate checkIn = checkInDate.toLocalDate();
        LocalDate checkOut = checkOutDate.toLocalDate();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }

        BigDecimal finalNightlyRate = baseNightlyRate
                .multiply(resolveDemandMultiplier(checkIn, checkOut, occupancyAboveThreshold))
                .setScale(2, RoundingMode.HALF_UP);

        return finalNightlyRate
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveDemandMultiplier(LocalDate checkIn, LocalDate checkOut, boolean occupancyAboveThreshold) {
        BigDecimal multiplier = BigDecimal.ONE;
        if (containsWeekend(checkIn, checkOut)) {
            multiplier = multiplier.multiply(WEEKEND_MULTIPLIER);
        }
        if (occupancyAboveThreshold) {
            multiplier = multiplier.multiply(OCCUPANCY_MULTIPLIER);
        }
        if (isWithinThreeDays(checkIn)) {
            multiplier = multiplier.multiply(SHORT_NOTICE_MULTIPLIER);
        }
        return multiplier;
    }

    private boolean containsWeekend(LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate cursor = checkIn; cursor.isBefore(checkOut); cursor = cursor.plusDays(1)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinThreeDays(LocalDate checkIn) {
        LocalDate today = LocalDate.now();
        return !checkIn.isBefore(today) && !checkIn.isAfter(today.plusDays(3));
    }
}
