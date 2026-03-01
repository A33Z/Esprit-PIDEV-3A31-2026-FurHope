package application.model;

import java.util.List;

public record HotelMapDatasetModel(
        String city,
        double defaultLatitude,
        double defaultLongitude,
        int totalHotels,
        List<HotelMapMarkerModel> markers
) {
    public HotelMapDatasetModel {
        markers = markers == null ? List.of() : List.copyOf(markers);
    }
}
