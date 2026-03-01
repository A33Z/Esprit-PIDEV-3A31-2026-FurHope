package integrations.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.esprit.config.AppConfig;
import integrations.http.ExternalApiException;
import integrations.http.HttpJsonClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OverpassHotelClient {

    private static final String NOMINATIM_ENDPOINT = "https://nominatim.openstreetmap.org/search";
    private static final String OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter";

    private final HttpJsonClient httpJsonClient;

    public OverpassHotelClient() {
        this.httpJsonClient = new HttpJsonClient();
    }

    public List<ExternalHotelCandidate> discoverHotels(String city, int limit) {
        if (city == null || city.trim().isEmpty()) {
            return List.of();
        }

        GeocodeResult center = geocode(city.trim());
        String query = buildOverpassQuery(center.latitude(), center.longitude(), AppConfig.overpassRadiusMeters());
        JsonNode payload = httpJsonClient.postTextForJson(OVERPASS_ENDPOINT, "text/plain", query);

        JsonNode elements = payload.path("elements");
        if (!elements.isArray()) {
            return List.of();
        }

        List<ExternalHotelCandidate> hotels = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            String name = text(tags, "name");
            if (name.isBlank()) {
                continue;
            }

            double latitude = coordinate(element, "lat", "center", "lat");
            double longitude = coordinate(element, "lon", "center", "lon");
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                continue;
            }

            String address = buildAddress(tags, center.displayName(), city.trim());
            String dedupeKey = (name + "|" + address).toLowerCase();
            if (!seen.add(dedupeKey)) {
                continue;
            }

            String sourceId = element.path("type").asText("node") + ":" + element.path("id").asText();
            ExternalHotelCandidate hotel = new ExternalHotelCandidate(
                    sourceId,
                    name.trim(),
                    city.trim(),
                    address,
                    latitude,
                    longitude,
                    parseStars(tags),
                    resolveDescription(tags),
                    resolvePrice(tags),
                    resolvePrimaryImage(tags)
            );
            hotels.add(hotel);

            if (hotels.size() >= limit) {
                break;
            }
        }

        return hotels;
    }

    private GeocodeResult geocode(String city) {
        String query = "format=jsonv2&limit=1&q=" + urlEncode(city);
        JsonNode response = httpJsonClient.getJson(NOMINATIM_ENDPOINT + "?" + query);

        if (!response.isArray() || response.isEmpty()) {
            throw new ExternalApiException("Could not geocode city: " + city);
        }

        JsonNode top = response.get(0);
        double latitude = parseDouble(top.path("lat").asText(""), Double.NaN);
        double longitude = parseDouble(top.path("lon").asText(""), Double.NaN);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            throw new ExternalApiException("Geocoding did not return valid coordinates.");
        }
        String displayName = top.path("display_name").asText(city);
        return new GeocodeResult(latitude, longitude, displayName);
    }

    private String buildOverpassQuery(double latitude, double longitude, int radiusMeters) {
        return """
                [out:json][timeout:25];
                (
                  node["tourism"="hotel"](around:%d,%f,%f);
                  way["tourism"="hotel"](around:%d,%f,%f);
                  relation["tourism"="hotel"](around:%d,%f,%f);
                );
                out center tags;
                """.formatted(
                radiusMeters, latitude, longitude,
                radiusMeters, latitude, longitude,
                radiusMeters, latitude, longitude
        );
    }

    private String resolveDescription(JsonNode tags) {
        String description = text(tags, "description:en");
        if (!description.isBlank()) {
            return description;
        }
        description = text(tags, "description");
        if (!description.isBlank()) {
            return description;
        }
        description = text(tags, "brand");
        if (!description.isBlank()) {
            return "Part of the " + description + " hospitality brand.";
        }
        return "";
    }

    private String resolvePrice(JsonNode tags) {
        String value = text(tags, "price");
        if (!value.isBlank()) {
            return value;
        }
        value = text(tags, "charge");
        if (!value.isBlank()) {
            return value;
        }
        return text(tags, "room:price");
    }

    private String resolvePrimaryImage(JsonNode tags) {
        String directImage = normalizeImageReference(text(tags, "image"));
        if (!directImage.isBlank()) {
            return directImage;
        }

        String galleryImage = normalizeImageReference(text(tags, "image:0"));
        if (!galleryImage.isBlank()) {
            return galleryImage;
        }

        String commonsFile = normalizeCommonsReference(text(tags, "wikimedia_commons"));
        if (!commonsFile.isBlank()) {
            return commonsFile;
        }
        return "";
    }

    private String normalizeCommonsReference(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String value = rawValue.trim();
        if (value.regionMatches(true, 0, "File:", 0, 5)) {
            return commonsFilePath(value.substring(5));
        }
        if (value.regionMatches(true, 0, "Category:", 0, 9)) {
            return "";
        }
        return commonsFilePath(value);
    }

    private String normalizeImageReference(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String value = rawValue.trim();
        String lower = value.toLowerCase();
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return value;
        }
        if (value.regionMatches(true, 0, "File:", 0, 5)) {
            return commonsFilePath(value.substring(5));
        }
        return "";
    }

    private String commonsFilePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String normalized = fileName.trim().replace('_', ' ');
        if (normalized.isBlank()) {
            return "";
        }
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + urlEncode(normalized);
    }

    private Double parseStars(JsonNode tags) {
        String value = text(tags, "stars");
        if (value.isBlank()) {
            value = text(tags, "hotel:stars");
        }
        if (value.isBlank()) {
            return null;
        }
        String numeric = value.replaceAll("[^0-9.]", "");
        if (numeric.isBlank()) {
            return null;
        }
        return parseDouble(numeric, null);
    }

    private String buildAddress(JsonNode tags, String geocodeDisplayName, String city) {
        String street = text(tags, "addr:street");
        String house = text(tags, "addr:housenumber");
        String locality = text(tags, "addr:city");
        if (locality.isBlank()) {
            locality = text(tags, "addr:town");
        }
        if (locality.isBlank()) {
            locality = city;
        }
        if (!street.isBlank()) {
            String prefix = house.isBlank() ? "" : house + " ";
            return (prefix + street + ", " + locality).trim();
        }
        if (!locality.isBlank()) {
            return locality;
        }
        return geocodeDisplayName;
    }

    private String text(JsonNode node, String key) {
        if (node == null || key == null) {
            return "";
        }
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private double coordinate(JsonNode element, String directField, String nestedObject, String nestedField) {
        if (element.hasNonNull(directField)) {
            return element.path(directField).asDouble(Double.NaN);
        }
        JsonNode nested = element.path(nestedObject);
        if (nested.hasNonNull(nestedField)) {
            return nested.path(nestedField).asDouble(Double.NaN);
        }
        return Double.NaN;
    }

    private Double parseDouble(String raw, Double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record GeocodeResult(double latitude, double longitude, String displayName) {
    }
}

