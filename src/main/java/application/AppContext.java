package application;

import application.controller.SupportChatController;
import application.service.HotelExplorationService;
import application.service.ManagerDashboardService;
import application.service.SupportAssistantService;
import application.service.UserReservationService;
import integrations.content.WikipediaContentClient;
import integrations.travel.OverpassHotelClient;
import integrations.weather.OpenMeteoWeatherClient;
import services.HotelAccessService;
import services.HotelService;
import services.ReservationAccessService;

public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private HotelExplorationService hotelExplorationService;
    private UserReservationService userReservationService;
    private ManagerDashboardService managerDashboardService;
    private SupportAssistantService supportAssistantService;
    private SupportChatController supportChatController;

    private AppContext() {
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public synchronized HotelExplorationService hotelExplorationService() {
        if (hotelExplorationService == null) {
            HotelService hotelService = new HotelService();
            hotelExplorationService = new HotelExplorationService(
                    hotelService,
                    new OverpassHotelClient(),
                    new WikipediaContentClient(),
                    new OpenMeteoWeatherClient()
            );
        }
        return hotelExplorationService;
    }

    public synchronized UserReservationService userReservationService() {
        if (userReservationService == null) {
            userReservationService = new UserReservationService(new ReservationAccessService(), hotelExplorationService());
        }
        return userReservationService;
    }

    public synchronized ManagerDashboardService managerDashboardService() {
        if (managerDashboardService == null) {
            managerDashboardService = new ManagerDashboardService(new HotelAccessService(), new ReservationAccessService());
        }
        return managerDashboardService;
    }

    public synchronized SupportAssistantService supportAssistantService() {
        if (supportAssistantService == null) {
            supportAssistantService = new SupportAssistantService(userReservationService(), hotelExplorationService());
        }
        return supportAssistantService;
    }

    public synchronized SupportChatController supportChatController() {
        if (supportChatController == null) {
            supportChatController = new SupportChatController(supportAssistantService());
        }
        return supportChatController;
    }
}
