package integrations.geo;

import com.fasterxml.jackson.databind.JsonNode;
import integrations.http.ExternalApiException;
import integrations.http.HttpJsonClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class NominatimGeocodingClient {

    private static final String NOMINATIM_ENDPOINT = "https://nominatim.openstreetmap.org/search";

    private final HttpJsonClient httpJsonClient;

    public NominatimGeocodingClient() {
        this.httpJsonClient = new HttpJsonClient();
    }

    public Optional<GeoPoint> geocode(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            String search = "format=jsonv2&limit=1&q=" + urlEncode(query.trim());
            JsonNode response = httpJsonClient.getJson(NOMINATIM_ENDPOINT + "?" + search);
            if (!response.isArray() || response.isEmpty()) {
                return Optional.empty();
            }

            JsonNode top = response.get(0);
            double latitude = parseDouble(top.path("lat").asText(""));
            double longitude = parseDouble(top.path("lon").asText(""));
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                return Optional.empty();
            }
            String displayName = top.path("display_name").asText(query.trim());
            return Optional.of(new GeoPoint(latitude, longitude, displayName));
        } catch (ExternalApiException e) {
            return Optional.empty();
        }
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record GeoPoint(double latitude, double longitude, String displayName) {
    }
}
