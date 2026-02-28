package application.service;

import application.model.ManagerAnalyticsModel;
import application.model.ManagerHotelInfoModel;
import application.model.ManagerReservationTicketModel;
import application.model.ManagerResourceModel;
import application.model.MonthlyReservationTrendModel;
import application.model.MonthlyRevenueTrendModel;
import entities.Hotel;
import entities.ReservationStatus;
import integrations.content.RealHotelImageCatalog;
import services.HotelAccessService;
import services.HotelAvailabilitySnapshot;
import services.HotelResourceType;
import services.ManagerAnalyticsSnapshot;
import services.ManagerReservationSnapshot;
import services.ManagerResourceEntry;
import services.MonthlyReservationTrendPoint;
import services.MonthlyRevenueTrendPoint;
import services.ReservationAccessService;
import services.ReservationDecisionResult;
import services.ResourceManagementService;
import services.UserReservationActionResult;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ManagerDashboardService {

    private final HotelAccessService hotelAccessService;
    private final ReservationAccessService reservationAccessService;
    private final ResourceManagementService resourceManagementService;

    public ManagerDashboardService(
            HotelAccessService hotelAccessService,
            ReservationAccessService reservationAccessService
    ) {
        this(hotelAccessService, reservationAccessService, new ResourceManagementService());
    }

    public ManagerDashboardService(
            HotelAccessService hotelAccessService,
            ReservationAccessService reservationAccessService,
            ResourceManagementService resourceManagementService
    ) {
        this.hotelAccessService = hotelAccessService;
        this.reservationAccessService = reservationAccessService;
        this.resourceManagementService = resourceManagementService;
    }

    public List<Hotel> getHotels() {
        return hotelAccessService.viewAllHotels();
    }

    public List<ManagerHotelInfoModel> getHotelInfoModelsForManager() {
        Map<Integer, HotelAvailabilitySnapshot> availabilityByHotelId = getHotelAvailabilityByHotelId();
        Map<Integer, BigDecimal> referencePriceByHotelId = mapReferencePriceByHotelId();
        return getHotels().stream()
                .map(hotel -> {
                    HotelAvailabilitySnapshot availability = availabilityByHotelId == null
                            ? null
                            : availabilityByHotelId.get(hotel.getId());
                    int totalRooms = availability == null
                            ? Math.max(0, hotel.getCapacity())
                            : Math.max(0, availability.totalRooms());
                    int availableRooms = availability == null
                            ? Math.max(0, hotel.getCapacity())
                            : Math.max(0, availability.availableRooms());
                    return new ManagerHotelInfoModel(
                            hotel.getId(),
                            normalizeHotelName(hotel.getName()),
                            normalizeLocation(hotel.getAddress()),
                            deriveStarRating(hotel.getName()),
                            totalRooms,
                            availableRooms,
                            normalizeAvailabilityStatus(availability == null ? null : availability.availabilityStatus()),
                            referencePriceByHotelId.get(hotel.getId()),
                            resolveThumbnailUrl(hotel)
                    );
                })
                .sorted(Comparator.comparing(ManagerHotelInfoModel::hotelName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean addHotel(Hotel hotel) {
        return hotelAccessService.addHotel(hotel);
    }

    public boolean updateHotel(Hotel hotel) {
        return hotelAccessService.updateHotel(hotel);
    }

    public boolean deleteHotel(int hotelId) {
        return hotelAccessService.deleteHotel(hotelId);
    }

    public Map<Integer, HotelAvailabilitySnapshot> getHotelAvailabilityByHotelId() {
        return reservationAccessService.viewHotelAvailabilityForManager();
    }

    public List<ManagerReservationTicketModel> getReservationTicketsForManager(int page, int pageSize) {
        return reservationAccessService.viewReservationPageForManager(page, pageSize).stream()
                .map(this::toReservationTicketModel)
                .toList();
    }

    public int countReservationsForManager() {
        return reservationAccessService.countReservationsForManager();
    }

    public List<ManagerReservationTicketModel> getActiveReservationTicketsForManager() {
        final int pageSize = 200;
        int totalReservations = Math.max(0, countReservationsForManager());
        if (totalReservations == 0) {
            return List.of();
        }

        int totalPages = (int) Math.ceil(totalReservations / (double) pageSize);
        List<ManagerReservationTicketModel> tickets = new ArrayList<>(totalReservations);
        for (int page = 0; page < totalPages; page++) {
            List<ManagerReservationTicketModel> pageItems = getReservationTicketsForManager(page, pageSize);
            if (pageItems.isEmpty()) {
                break;
            }
            tickets.addAll(pageItems);
        }
        return tickets.stream()
                .filter(Objects::nonNull)
                .filter(ticket -> ticket.status() == ReservationStatus.PENDING
                        || ticket.status() == ReservationStatus.APPROVED
                        || ticket.status() == ReservationStatus.DECLINED)
                .toList();
    }

    public ManagerAnalyticsModel getAnalyticsForManager() {
        ManagerAnalyticsSnapshot snapshot = reservationAccessService.viewManagerAnalytics();
        if (snapshot == null) {
            return new ManagerAnalyticsModel(
                    BigDecimal.ZERO,
                    0,
                    "N/A",
                    0.0,
                    List.of(),
                    List.of()
            );
        }
        List<MonthlyReservationTrendModel> monthlyTrend = snapshot.monthlyTrend() == null
                ? List.of()
                : snapshot.monthlyTrend().stream()
                .map(this::toMonthlyTrendModel)
                .toList();
        List<MonthlyRevenueTrendModel> revenueTrend = snapshot.revenueTrend() == null
                ? List.of()
                : snapshot.revenueTrend().stream()
                .map(this::toRevenueTrendModel)
                .toList();

        return new ManagerAnalyticsModel(
                snapshot.totalRevenue(),
                snapshot.totalReservations(),
                normalizeHotelName(snapshot.mostBookedHotel()),
                Math.max(0.0, snapshot.averageOccupancyRate()),
                monthlyTrend,
                revenueTrend
        );
    }

    public ReservationDecisionResult decideReservationStatus(int reservationId, ReservationStatus targetStatus) {
        return reservationAccessService.decideReservationStatus(reservationId, targetStatus);
    }

    public UserReservationActionResult modifyReservationByManager(int reservationId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return UserReservationActionResult.invalidDates();
        }
        return reservationAccessService.modifyReservationDatesByManager(
                reservationId,
                Date.valueOf(checkInDate),
                Date.valueOf(checkOutDate)
        );
    }

    public UserReservationActionResult cancelReservationByManager(int reservationId) {
        return reservationAccessService.cancelReservationByManager(reservationId);
    }

    public List<ManagerResourceModel> getResourceModelsForManager() {
        Map<Integer, String> hotelNamesById = new HashMap<>();
        for (Hotel hotel : getHotels()) {
            hotelNamesById.put(hotel.getId(), normalizeHotelName(hotel.getName()));
        }
        return resourceManagementService.listAllResources().stream()
                .map(entry -> toResourceModel(entry, hotelNamesById))
                .filter(Objects::nonNull)
                .toList();
    }

    public ManagerResourceModel addResourceForManager(
            int hotelId,
            HotelResourceType resourceType,
            String resourceName,
            BigDecimal unitPrice,
            int capacity,
            boolean available
    ) {
        ManagerResourceEntry entry = resourceManagementService.addResource(
                hotelId, resourceType, resourceName, unitPrice, capacity, available
        );
        return toResourceModel(entry, mapHotelNames());
    }

    public ManagerResourceModel updateResourceForManager(
            String resourceToken,
            int hotelId,
            HotelResourceType resourceType,
            String resourceName,
            BigDecimal unitPrice,
            int capacity,
            boolean available
    ) {
        ManagerResourceEntry entry = resourceManagementService.updateResource(
                resourceToken, hotelId, resourceType, resourceName, unitPrice, capacity, available
        );
        return toResourceModel(entry, mapHotelNames());
    }

    public boolean deleteResourceForManager(String resourceToken) {
        return resourceManagementService.deleteResource(resourceToken);
    }

    private ManagerReservationTicketModel toReservationTicketModel(ManagerReservationSnapshot snapshot) {
        return new ManagerReservationTicketModel(
                snapshot.reservationId(),
                normalizeHotelName(snapshot.hotelName()),
                normalizeGuestName(snapshot.guestName()),
                toLocalDate(snapshot.reservationDate()),
                toLocalDate(snapshot.checkInDate()),
                toLocalDate(snapshot.checkOutDate()),
                Math.max(0, snapshot.nights()),
                snapshot.totalPrice() == null ? BigDecimal.ZERO : snapshot.totalPrice(),
                Math.max(1, snapshot.guestCount()),
                snapshot.status()
        );
    }

    private ManagerResourceModel toResourceModel(ManagerResourceEntry entry, Map<Integer, String> hotelNamesById) {
        if (entry == null) {
            return null;
        }
        String hotelName = hotelNamesById.getOrDefault(entry.hotelId(), "Unknown Hotel");
        return new ManagerResourceModel(
                entry.resourceToken(),
                entry.hotelId(),
                hotelName,
                entry.resourceType().name(),
                entry.resourceName(),
                entry.unitPrice(),
                entry.capacity(),
                entry.available()
        );
    }

    private Map<Integer, String> mapHotelNames() {
        Map<Integer, String> names = new HashMap<>();
        for (Hotel hotel : getHotels()) {
            names.put(hotel.getId(), normalizeHotelName(hotel.getName()));
        }
        return names;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private String normalizeHotelName(String hotelName) {
        if (hotelName == null || hotelName.trim().isEmpty()) {
            return "Unknown Hotel";
        }
        return hotelName.trim();
    }

    private String normalizeGuestName(String guestName) {
        if (guestName == null || guestName.trim().isEmpty()) {
            return "Registered Guest";
        }
        return guestName.trim();
    }

    private String normalizeLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return "Location unavailable";
        }
        return location.trim();
    }

    private String normalizeAvailabilityStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Unknown";
        }
        return status.trim();
    }

    private double deriveStarRating(String seed) {
        if (seed == null || seed.isBlank()) {
            return 4.0;
        }
        int hash = Math.abs(seed.trim().toLowerCase(Locale.US).hashCode());
        double rawRating = 3.5 + (hash % 16) / 10.0;
        return Math.round(Math.min(5.0, rawRating) * 10.0) / 10.0;
    }

    private Map<Integer, BigDecimal> mapReferencePriceByHotelId() {
        Map<Integer, BigDecimal> referencePriceByHotelId = new HashMap<>();
        try {
            resourceManagementService.listAllResources().forEach(resource -> {
                if (resource == null || resource.hotelId() <= 0 || resource.unitPrice() == null) {
                    return;
                }
                if (resource.resourceType() != HotelResourceType.ROOM) {
                    return;
                }
                if (resource.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                referencePriceByHotelId.merge(
                        resource.hotelId(),
                        resource.unitPrice(),
                        BigDecimal::min
                );
            });
        } catch (RuntimeException ignored) {
            // Optional enrichment only; UI safely handles missing prices.
        }
        return referencePriceByHotelId;
    }

    private String resolveThumbnailUrl(Hotel hotel) {
        if (hotel == null) {
            return "";
        }
        int seed = hotel.getId() > 0 ? hotel.getId() : Math.abs(normalizeHotelName(hotel.getName()).hashCode());
        return RealHotelImageCatalog.bySeed(seed);
    }

    private MonthlyReservationTrendModel toMonthlyTrendModel(MonthlyReservationTrendPoint point) {
        if (point == null) {
            return new MonthlyReservationTrendModel("", 0);
        }
        return new MonthlyReservationTrendModel(point.monthLabel(), Math.max(0, point.reservationCount()));
    }

    private MonthlyRevenueTrendModel toRevenueTrendModel(MonthlyRevenueTrendPoint point) {
        if (point == null) {
            return new MonthlyRevenueTrendModel("", BigDecimal.ZERO);
        }
        return new MonthlyRevenueTrendModel(
                point.monthLabel(),
                point.totalRevenue() == null ? BigDecimal.ZERO : point.totalRevenue()
        );
    }
}
