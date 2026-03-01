package entities;

public enum ReservationStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED;

    public static ReservationStatus fromDatabase(String rawStatus) {
        if (rawStatus == null) {
            return PENDING;
        }

        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "APPROVED", "CONFIRMED" -> APPROVED;
            case "DECLINED" -> DECLINED;
            case "CANCELLED" -> CANCELLED;
            default -> PENDING;
        };
    }
}
