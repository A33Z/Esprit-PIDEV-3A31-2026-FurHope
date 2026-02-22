package entities;

public enum ReservationStatus {
    PENDING,
    APPROVED,
    DECLINED;

    public static ReservationStatus fromDatabase(String rawStatus) {
        if (rawStatus == null) {
            return PENDING;
        }

        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "APPROVED", "CONFIRMED" -> APPROVED;
            case "DECLINED", "CANCELLED" -> DECLINED;
            default -> PENDING;
        };
    }
}
