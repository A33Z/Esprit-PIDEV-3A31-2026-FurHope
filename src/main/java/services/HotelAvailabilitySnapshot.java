package services;

public record HotelAvailabilitySnapshot(
        int hotelId,
        int totalRooms,
        int occupiedRooms,
        int availableRooms,
        double occupancyRate,
        String availabilityStatus
) {
}
