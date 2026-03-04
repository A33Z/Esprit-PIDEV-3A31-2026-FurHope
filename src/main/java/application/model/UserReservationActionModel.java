package application.model;

import services.UserReservationActionCode;

public record UserReservationActionModel(
        UserReservationActionCode code,
        UserReservationTicketModel ticket
) {
    public boolean isSuccess() {
        return code == UserReservationActionCode.UPDATED || code == UserReservationActionCode.CANCELLED;
    }
}
