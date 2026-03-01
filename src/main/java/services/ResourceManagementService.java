package services;

import entities.Hotel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManagementService {

    private static final int MAX_RESOURCE_NAME_LENGTH = 120;
    private static final int MAX_CAPACITY = 100_000;
    private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("1000000.00");

    private static final Map<String, ManagerResourceEntry> RESOURCE_STORE = new ConcurrentHashMap<>();

    private final HotelService hotelService = new HotelService();

    public List<ManagerResourceEntry> listAllResources() {
        SessionContext.requireManager();
        return RESOURCE_STORE.values().stream()
                .sorted(Comparator.comparing(ManagerResourceEntry::updatedAt).reversed())
                .toList();
    }

    public ManagerResourceEntry addResource(
            int hotelId,
            HotelResourceType resourceType,
            String resourceName,
            BigDecimal unitPrice,
            int capacity,
            boolean available
    ) {
        SessionContext.requireManager();
        validatePayload(hotelId, resourceType, resourceName, unitPrice, capacity);

        ManagerResourceEntry entry = new ManagerResourceEntry(
                UUID.randomUUID().toString(),
                hotelId,
                resourceType,
                sanitizeResourceName(resourceName),
                normalizePrice(unitPrice),
                capacity,
                available,
                Instant.now()
        );
        RESOURCE_STORE.put(entry.resourceToken(), entry);
        return entry;
    }

    public ManagerResourceEntry updateResource(
            String resourceToken,
            int hotelId,
            HotelResourceType resourceType,
            String resourceName,
            BigDecimal unitPrice,
            int capacity,
            boolean available
    ) {
        SessionContext.requireManager();
        if (resourceToken == null || resourceToken.isBlank()) {
            throw new IllegalArgumentException("Resource reference is required.");
        }
        validatePayload(hotelId, resourceType, resourceName, unitPrice, capacity);

        ManagerResourceEntry existing = RESOURCE_STORE.get(resourceToken);
        if (existing == null) {
            throw new IllegalArgumentException("Resource does not exist.");
        }

        ManagerResourceEntry updated = new ManagerResourceEntry(
                existing.resourceToken(),
                hotelId,
                resourceType,
                sanitizeResourceName(resourceName),
                normalizePrice(unitPrice),
                capacity,
                available,
                Instant.now()
        );
        RESOURCE_STORE.put(updated.resourceToken(), updated);
        return updated;
    }

    public boolean deleteResource(String resourceToken) {
        SessionContext.requireManager();
        if (resourceToken == null || resourceToken.isBlank()) {
            return false;
        }
        return RESOURCE_STORE.remove(resourceToken) != null;
    }

    private void validatePayload(
            int hotelId,
            HotelResourceType resourceType,
            String resourceName,
            BigDecimal unitPrice,
            int capacity
    ) {
        if (hotelId <= 0 || !hotelExists(hotelId)) {
            throw new IllegalArgumentException("A valid hotel selection is required.");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type is required.");
        }
        String normalizedName = sanitizeResourceName(resourceName);
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Resource name is required.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0 || unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new IllegalArgumentException("Unit price must be greater than 0 and within allowed range.");
        }
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_CAPACITY + ".");
        }
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String sanitizeResourceName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_RESOURCE_NAME_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_RESOURCE_NAME_LENGTH);
    }

    private boolean hotelExists(int hotelId) {
        Hotel hotel = hotelService.getHotelById(hotelId);
        return hotel != null;
    }
}
