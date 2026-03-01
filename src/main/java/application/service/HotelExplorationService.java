package application.service;

import application.model.HotelCardModel;
import application.model.HotelDetailsModel;
import application.model.HotelMapDatasetModel;
import application.model.HotelMapMarkerModel;
import com.esprit.config.AppConfig;
import entities.Hotel;
import integrations.content.RealHotelImageCatalog;
import integrations.content.WikiContent;
import integrations.content.WikipediaContentClient;
import integrations.geo.NominatimGeocodingClient;
import integrations.http.ExternalApiException;
import integrations.travel.ExternalHotelCandidate;
import integrations.travel.OverpassHotelClient;
import integrations.weather.OpenMeteoWeatherClient;
import services.HotelService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class HotelExplorationService {

    private final HotelService hotelService;
    private final OverpassHotelClient overpassHotelClient;
    private final WikipediaContentClient wikipediaContentClient;
    private final OpenMeteoWeatherClient openMeteoWeatherClient;
    private final NominatimGeocodingClient nominatimGeocodingClient;

    private final Map<Integer, HotelDetailsModel> detailsCache = new ConcurrentHashMap<>();
    private final Map<String, Optional<NominatimGeocodingClient.GeoPoint>> geocodeCache = new ConcurrentHashMap<>();

    public HotelExplorationService(
            HotelService hotelService,
            OverpassHotelClient overpassHotelClient,
            WikipediaContentClient wikipediaContentClient,
            OpenMeteoWeatherClient openMeteoWeatherClient
    ) {
        this(
                hotelService,
                overpassHotelClient,
                wikipediaContentClient,
                openMeteoWeatherClient,
                new NominatimGeocodingClient()
        );
    }

    public HotelExplorationService(
            HotelService hotelService,
            OverpassHotelClient overpassHotelClient,
            WikipediaContentClient wikipediaContentClient,
            OpenMeteoWeatherClient openMeteoWeatherClient,
            NominatimGeocodingClient nominatimGeocodingClient
    ) {
        this.hotelService = hotelService;
        this.overpassHotelClient = overpassHotelClient;
        this.wikipediaContentClient = wikipediaContentClient;
        this.openMeteoWeatherClient = openMeteoWeatherClient;
        this.nominatimGeocodingClient = nominatimGeocodingClient == null
                ? new NominatimGeocodingClient()
                : nominatimGeocodingClient;
    }

    public List<HotelCardModel> discoverHotels(String rawCity) {
        String city = normalizeCity(rawCity);
        try {
            List<ExternalHotelCandidate> candidates;

            try {
                candidates = overpassHotelClient.discoverHotels(city, AppConfig.hotelSearchLimit());
            } catch (ExternalApiException e) {
                candidates = List.of();
            }

            if (candidates.isEmpty()) {
                return buildFallbackCards(city);
            }

            List<HotelCardModel> cards = new ArrayList<>();
            for (ExternalHotelCandidate candidate : candidates) {
                try {
                    cards.add(buildCard(candidate, city));
                } catch (RuntimeException ignored) {
                    // Skip malformed/external entries instead of failing the full dashboard render.
                }
            }

            if (cards.isEmpty()) {
                return buildFallbackCards(city);
            }
            cards.sort(Comparator.comparingDouble(HotelCardModel::rating).reversed());
            return cards;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    public HotelDetailsModel getHotelDetails(int hotelId) {
        HotelDetailsModel cached = detailsCache.get(hotelId);
        if (cached == null) {
            cached = buildFallbackDetails(hotelId);
            if (cached == null) {
                return null;
            }
            detailsCache.put(hotelId, cached);
        }

        String weather = openMeteoWeatherClient.getCurrentWeatherSummary(cached.latitude(), cached.longitude());
        return new HotelDetailsModel(
                cached.hotelId(),
                cached.name(),
                cached.rating(),
                cached.fullDescription(),
                cached.priceLabel(),
                cached.location(),
                cached.latitude(),
                cached.longitude(),
                cached.imageUrls(),
                weather
        );
    }

    public List<HotelMapMarkerModel> toMapMarkers(List<HotelCardModel> cards) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .filter(card -> !Double.isNaN(card.latitude()) && !Double.isNaN(card.longitude()))
                .map(card -> new HotelMapMarkerModel(
                        card.hotelId(),
                        sanitizeMapText(card.name(), "Hotel", 120),
                        sanitizeMapText(card.location(), AppConfig.defaultCity(), 220),
                        0,
                        sanitizeMapText(card.shortDescription(), "", 220),
                        card.latitude(),
                        card.longitude()
                ))
                .collect(Collectors.toList());
    }

    public HotelMapDatasetModel loadDatabaseMapDataset(String rawCity) {
        String city = normalizeCity(rawCity);
        List<Hotel> hotels = hotelService.getAllHotels();
        Map<Integer, HotelService.HotelGeoPoint> storedCoordinates = hotelService.getHotelCoordinatesIfAvailable();
        Optional<NominatimGeocodingClient.GeoPoint> cityCenter = resolveCityCenter(city);

        if (hotels == null || hotels.isEmpty()) {
            return new HotelMapDatasetModel(
                    city,
                    resolveDefaultLatitude(cityCenter, null),
                    resolveDefaultLongitude(cityCenter, null),
                    0,
                    List.of()
            );
        }

        List<HotelMapMarkerModel> markers = new ArrayList<>();
        for (Hotel hotel : hotels) {
            if (hotel == null || hotel.getId() <= 0) {
                continue;
            }

            Optional<NominatimGeocodingClient.GeoPoint> point = resolveHotelCoordinates(hotel, storedCoordinates, city);
            if (point.isEmpty()) {
                continue;
            }

            String hotelName = sanitizeMapText(hotel.getName(), "Hotel", 120);
            String address = sanitizeMapText(normalizeAddress(hotel.getAddress(), city), city, 220);
            int capacity = Math.max(0, hotel.getCapacity());
            String description = capacity > 0
                    ? "Capacity: " + capacity + " guests"
                    : "";

            markers.add(new HotelMapMarkerModel(
                    hotel.getId(),
                    hotelName,
                    address,
                    capacity,
                    description,
                    point.get().latitude(),
                    point.get().longitude()
            ));
        }

        markers.sort(Comparator.comparing(HotelMapMarkerModel::name, String.CASE_INSENSITIVE_ORDER));

        NominatimGeocodingClient.GeoPoint firstMarkerPoint = markers.isEmpty()
                ? null
                : new NominatimGeocodingClient.GeoPoint(
                markers.get(0).latitude(),
                markers.get(0).longitude(),
                city
        );

        return new HotelMapDatasetModel(
                city,
                resolveDefaultLatitude(cityCenter, firstMarkerPoint),
                resolveDefaultLongitude(cityCenter, firstMarkerPoint),
                hotels.size(),
                markers
        );
    }

    public String resolveHotelName(int hotelId) {
        HotelDetailsModel cached = detailsCache.get(hotelId);
        if (cached != null) {
            return cached.name();
        }
        Hotel hotel = hotelService.getHotelById(hotelId);
        return hotel == null ? "Hotel #" + hotelId : hotel.getName();
    }

    public BigDecimal resolveNightlyRate(int hotelId) {
        HotelDetailsModel details = detailsCache.get(hotelId);
        if (details == null) {
            details = buildFallbackDetails(hotelId);
            if (details != null) {
                detailsCache.put(hotelId, details);
            }
        }
        if (details == null) {
            return new BigDecimal("85.00");
        }

        BigDecimal parsed = parseRate(details.priceLabel());
        if (parsed == null || parsed.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("85.00");
        }
        return parsed.setScale(2, RoundingMode.HALF_UP);
    }

    private HotelCardModel buildCard(ExternalHotelCandidate candidate, String city) {
        int hotelId = hotelService.ensureHotelRecord(candidate.name(), normalizeAddress(candidate.address(), city));
        WikiContent wikiContent = wikipediaContentClient.fetchHotelContent(candidate.name(), city);

        double rating = deriveRating(candidate.stars(), candidate.sourceId());
        String priceLabel = resolvePrice(candidate.rawPriceTag(), rating);
        String location = normalizeAddress(candidate.address(), city);
        String description = cleanDescription(resolveShortDescription(candidate, wikiContent, city), city);

        List<String> images = resolveImages(hotelId, candidate, wikiContent);
        String fullDescription = cleanDescription(resolveFullDescription(candidate, wikiContent, city), city);

        HotelDetailsModel details = new HotelDetailsModel(
                hotelId,
                candidate.name(),
                rating,
                fullDescription,
                priceLabel,
                location,
                candidate.latitude(),
                candidate.longitude(),
                images,
                "Weather unavailable"
        );
        detailsCache.put(hotelId, details);

        return new HotelCardModel(
                hotelId,
                candidate.name(),
                rating,
                truncate(description, 220),
                priceLabel,
                images.get(0),
                location,
                candidate.latitude(),
                candidate.longitude()
        );
    }

    private List<HotelCardModel> buildFallbackCards(String city) {
        List<Hotel> hotels = hotelService.getAllHotels();
        if (hotels.isEmpty()) {
            return List.of();
        }

        List<HotelCardModel> cards = new ArrayList<>();
        int max = Math.min(AppConfig.hotelSearchLimit(), hotels.size());
        for (int i = 0; i < max; i++) {
            Hotel hotel = hotels.get(i);
            double rating = deriveRating(null, hotel.getName() + hotel.getId());
            String description = cleanDescription("Comfort-focused stay in " + city + ".", city);
            String image = fallbackImage(hotel.getId());
            String priceLabel = resolvePrice("", rating);
            String location = normalizeAddress(hotel.getAddress(), city);

            detailsCache.put(
                    hotel.getId(),
                    new HotelDetailsModel(
                            hotel.getId(),
                            hotel.getName(),
                            rating,
                            description,
                            priceLabel,
                            location,
                            Double.NaN,
                            Double.NaN,
                            List.of(image),
                            "Weather unavailable"
                    )
            );

            cards.add(new HotelCardModel(
                    hotel.getId(),
                    hotel.getName(),
                    rating,
                    description,
                    priceLabel,
                    image,
                    location,
                    Double.NaN,
                    Double.NaN
            ));
        }
        return cards;
    }

    private HotelDetailsModel buildFallbackDetails(int hotelId) {
        Hotel hotel = hotelService.getHotelById(hotelId);
        if (hotel == null) {
            return null;
        }
        String image = fallbackImage(hotelId);
        String location = normalizeAddress(hotel.getAddress(), AppConfig.defaultCity());
        String description = cleanDescription("No external details available for this hotel yet.", AppConfig.defaultCity());
        return new HotelDetailsModel(
                hotel.getId(),
                hotel.getName(),
                deriveRating(null, hotel.getName() + hotel.getId()),
                description,
                resolvePrice("", 4.0),
                location,
                Double.NaN,
                Double.NaN,
                List.of(image),
                "Weather unavailable"
        );
    }

    private String resolveShortDescription(ExternalHotelCandidate candidate, WikiContent wikiContent, String city) {
        if (wikiContent.shortDescription() != null && !wikiContent.shortDescription().isBlank()) {
            return cleanDescription(wikiContent.shortDescription(), city);
        }
        if (candidate.description() != null && !candidate.description().isBlank()) {
            return cleanDescription(candidate.description(), city);
        }
        return cleanDescription("Comfort-focused hotel stay in " + city + ".", city);
    }

    private String resolveFullDescription(ExternalHotelCandidate candidate, WikiContent wikiContent, String city) {
        if (wikiContent.fullDescription() != null && !wikiContent.fullDescription().isBlank()) {
            return cleanDescription(wikiContent.fullDescription(), city);
        }
        if (candidate.description() != null && !candidate.description().isBlank()) {
            return cleanDescription(candidate.description(), city);
        }
        return cleanDescription("This property offers accommodation services in " + city + ".", city);
    }

    private List<String> resolveImages(int hotelId, ExternalHotelCandidate candidate, WikiContent wikiContent) {
        Set<String> deduplicated = new LinkedHashSet<>();

        if (candidate != null && candidate.primaryImageUrl() != null && !candidate.primaryImageUrl().isBlank()) {
            deduplicated.add(candidate.primaryImageUrl());
        }
        if (wikiContent.heroImageUrl() != null && !wikiContent.heroImageUrl().isBlank()) {
            deduplicated.add(wikiContent.heroImageUrl());
        }
        for (String url : wikiContent.galleryImageUrls()) {
            if (url != null && !url.isBlank()) {
                deduplicated.add(url);
            }
            if (deduplicated.size() >= 6) {
                break;
            }
        }

        if (deduplicated.isEmpty()) {
            return List.of(fallbackImage(hotelId));
        }
        return new ArrayList<>(deduplicated);
    }

    private String resolvePrice(String rawPrice, double rating) {
        if (rawPrice != null && !rawPrice.trim().isEmpty()) {
            String normalized = rawPrice.trim();
            if (normalized.toLowerCase().contains("night")) {
                return normalized;
            }
            return normalized + " / night";
        }
        int estimated = (int) Math.round(55 + (rating * 28));
        return "$" + estimated + " / night (estimated)";
    }

    private double deriveRating(Double stars, String seed) {
        if (stars != null) {
            if (stars <= 0) {
                return 3.9;
            }
            if (stars > 5.0) {
                return roundToOneDecimal(Math.min(5.0, stars / 2.0));
            }
            return roundToOneDecimal(Math.min(5.0, stars));
        }
        int stable = Math.abs(seed == null ? 0 : seed.hashCode());
        return roundToOneDecimal(3.5 + ((stable % 15) / 10.0));
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String normalized = cleanDescription(value, AppConfig.defaultCity());
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max - 3).trim() + "...";
    }

    private String cleanDescription(String value, String city) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("(?i)reliable\\s+pet\\s*-?\\s*friendly\\s+stay(\\s+in\\s+[^.]+)?\\.?", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return "Comfort-focused hotel stay in " + normalizeCity(city) + ".";
        }
        return normalized;
    }

    private String normalizeCity(String rawCity) {
        if (rawCity == null || rawCity.trim().isEmpty()) {
            return AppConfig.defaultCity();
        }
        return rawCity.trim();
    }

    private String normalizeAddress(String rawAddress, String city) {
        if (rawAddress == null || rawAddress.trim().isEmpty()) {
            return city;
        }
        return rawAddress.trim();
    }

    private Optional<NominatimGeocodingClient.GeoPoint> resolveCityCenter(String city) {
        String normalizedCity = normalizeCity(city);
        return geocodeWithCache(normalizedCity);
    }

    private Optional<NominatimGeocodingClient.GeoPoint> resolveHotelCoordinates(
            Hotel hotel,
            Map<Integer, HotelService.HotelGeoPoint> storedCoordinates,
            String city
    ) {
        if (hotel == null) {
            return Optional.empty();
        }

        HotelService.HotelGeoPoint stored = storedCoordinates == null ? null : storedCoordinates.get(hotel.getId());
        if (stored != null && isValidCoordinate(stored.latitude(), stored.longitude())) {
            return Optional.of(new NominatimGeocodingClient.GeoPoint(stored.latitude(), stored.longitude(), ""));
        }

        String primaryQuery = normalizeAddress(hotel.getAddress(), city);
        Optional<NominatimGeocodingClient.GeoPoint> byAddress = geocodeWithCache(primaryQuery);
        if (byAddress.isPresent()) {
            return byAddress;
        }

        String scopedQuery = primaryQuery.toLowerCase().contains(city.toLowerCase())
                ? primaryQuery
                : primaryQuery + ", " + city;
        Optional<NominatimGeocodingClient.GeoPoint> byScopedAddress = geocodeWithCache(scopedQuery);
        if (byScopedAddress.isPresent()) {
            return byScopedAddress;
        }

        String byName = sanitizeMapText(hotel.getName(), "Hotel", 120) + ", " + city;
        return geocodeWithCache(byName);
    }

    private Optional<NominatimGeocodingClient.GeoPoint> geocodeWithCache(String query) {
        String normalizedQuery = compactText(query);
        if (normalizedQuery.isBlank()) {
            return Optional.empty();
        }
        return geocodeCache.computeIfAbsent(
                normalizedQuery.toLowerCase(),
                key -> nominatimGeocodingClient.geocode(normalizedQuery)
        );
    }

    private String sanitizeMapText(String value, String fallback, int maxLength) {
        String normalized = compactText(value);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String compactText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private double resolveDefaultLatitude(
            Optional<NominatimGeocodingClient.GeoPoint> cityCenter,
            NominatimGeocodingClient.GeoPoint firstMarkerPoint
    ) {
        if (cityCenter != null && cityCenter.isPresent()) {
            return cityCenter.get().latitude();
        }
        if (firstMarkerPoint != null && isValidCoordinate(firstMarkerPoint.latitude(), firstMarkerPoint.longitude())) {
            return firstMarkerPoint.latitude();
        }
        return 40.7128;
    }

    private double resolveDefaultLongitude(
            Optional<NominatimGeocodingClient.GeoPoint> cityCenter,
            NominatimGeocodingClient.GeoPoint firstMarkerPoint
    ) {
        if (cityCenter != null && cityCenter.isPresent()) {
            return cityCenter.get().longitude();
        }
        if (firstMarkerPoint != null && isValidCoordinate(firstMarkerPoint.latitude(), firstMarkerPoint.longitude())) {
            return firstMarkerPoint.longitude();
        }
        return -74.0060;
    }

    private String fallbackImage(int hotelId) {
        return RealHotelImageCatalog.bySeed(Math.max(1, hotelId));
    }

    private BigDecimal parseRate(String priceLabel) {
        if (priceLabel == null || priceLabel.isBlank()) {
            return null;
        }
        String firstNumber = extractFirstNumber(priceLabel);
        if (firstNumber == null) {
            return null;
        }
        try {
            return new BigDecimal(firstNumber);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractFirstNumber(String value) {
        StringBuilder builder = new StringBuilder();
        boolean started = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                builder.append(c);
                started = true;
                continue;
            }
            if (started && c == '.' && builder.indexOf(".") < 0) {
                builder.append(c);
                continue;
            }
            if (started) {
                break;
            }
        }
        String extracted = builder.toString();
        return extracted.isBlank() ? null : extracted;
    }
}

