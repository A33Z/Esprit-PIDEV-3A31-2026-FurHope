package services;

import entities.ReservationStatus;

public record ReservationDecisionResult(
        ReservationDecisionCode code,
        ReservationStatus resultingStatus
) {

    public static ReservationDecisionResult updated(ReservationStatus status) {
        return new ReservationDecisionResult(ReservationDecisionCode.UPDATED, status);
    }

    public static ReservationDecisionResult notFound() {
        return new ReservationDecisionResult(ReservationDecisionCode.NOT_FOUND, null);
    }

    public static ReservationDecisionResult alreadyProcessed(ReservationStatus status) {
        return new ReservationDecisionResult(ReservationDecisionCode.ALREADY_PROCESSED, status);
    }

    public static ReservationDecisionResult invalidTarget() {
        return new ReservationDecisionResult(ReservationDecisionCode.INVALID_TARGET_STATUS, null);
    }

    public static ReservationDecisionResult failed() {
        return new ReservationDecisionResult(ReservationDecisionCode.FAILED, null);
    }

    public boolean isUpdated() {
        return code == ReservationDecisionCode.UPDATED;
    }
}
