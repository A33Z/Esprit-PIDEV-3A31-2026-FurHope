package integrations.travel;

public record ExternalHotelCandidate(
        String sourceId,
        String name,
        String city,
        String address,
        double latitude,
        double longitude,
        Double stars,
        String description,
        String rawPriceTag,
        String primaryImageUrl
) {
}
