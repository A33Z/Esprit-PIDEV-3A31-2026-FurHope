package controllers;

import application.AppContext;
import application.model.ManagerAnalyticsModel;
import application.model.ManagerHotelInfoModel;
import application.service.ManagerDashboardService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.AuthorizationException;
import services.SessionContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AnalyticsController {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label sessionLabel;

    @FXML
    private FlowPane metricsFlowPane;
    @FXML
    private FlowPane chartsFlowPane;

    @FXML
    private Label totalRevenueValueLabel;
    @FXML
    private Label totalReservationsValueLabel;
    @FXML
    private Label mostBookedHotelValueLabel;
    @FXML
    private Label averageOccupancyValueLabel;

    @FXML
    private LineChart<String, Number> monthlyReservationTrendChart;
    @FXML
    private BarChart<String, Number> monthlyRevenueTrendChart;
    @FXML
    private PieChart occupancyRatePieChart;
    @FXML
    private Label occupancySummaryLabel;

    private ManagerDashboardService managerDashboardService;
    private Timeline autoRefreshTimeline;
    private List<ManagerHotelInfoModel> allHotels = List.of();

    @FXML
    public void initialize() {
        try {
            SessionContext.requireManager();
        } catch (AuthorizationException e) {
            Platform.runLater(this::redirectToRoleSelection);
            return;
        }

        try {
            managerDashboardService = AppContext.getInstance().managerDashboardService();
        } catch (RuntimeException e) {
            managerDashboardService = null;
        }

        try {
            var manager = SessionContext.requireManager();
            sessionLabel.setText("Logged in as: " + manager.getDisplayName());
        } catch (RuntimeException ignored) {
            sessionLabel.setText("Manager session");
        }

        configureCharts();
        configureResponsiveLayout();
        refreshAnalyticsData();
        startAutoRefresh();
    }

    private void configureCharts() {
        monthlyReservationTrendChart.setAnimated(false);
        monthlyRevenueTrendChart.setAnimated(false);
        occupancyRatePieChart.setLabelsVisible(true);
    }

    private void configureResponsiveLayout() {
        if (rootPane == null) {
            return;
        }
        rootPane.widthProperty().addListener((obs, oldWidth, newWidth) -> applyResponsiveLayout(newWidth.doubleValue()));
        Platform.runLater(() -> applyResponsiveLayout(rootPane.getWidth()));
    }

    private void applyResponsiveLayout(double width) {
        double wrapLength = Math.max(760.0, width - 80.0);
        if (metricsFlowPane != null) {
            metricsFlowPane.setPrefWrapLength(wrapLength);
        }
        if (chartsFlowPane != null) {
            chartsFlowPane.setPrefWrapLength(wrapLength);
        }
    }

    @FXML
    private void handleRefreshAnalytics() {
        refreshAnalyticsData();
    }

    @FXML
    private void handleBackToDashboard() {
        stopAutoRefresh();
        navigateTo("/HotelManagerDashboard.fxml", "FurHope - Hotel Manager Dashboard");
    }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        SessionContext.logout();
        navigateTo("/RoleSelection.fxml", "FurHope - Access Portal");
    }

    private void refreshAnalyticsData() {
        if (managerDashboardService == null) {
            applyAnalyticsFallback();
            return;
        }

        try {
            allHotels = managerDashboardService.getHotelInfoModelsForManager();
            ManagerAnalyticsModel analytics = managerDashboardService.getAnalyticsForManager();

            totalRevenueValueLabel.setText(formatCurrency(analytics.totalRevenue()));
            totalReservationsValueLabel.setText(String.valueOf(Math.max(0, analytics.totalReservations())));
            mostBookedHotelValueLabel.setText(safeDisplay(analytics.mostBookedHotel(), "N/A"));
            averageOccupancyValueLabel.setText(String.format(Locale.US, "%.1f%%", Math.max(0.0, analytics.averageOccupancyRate())));

            renderReservationTrendChart(analytics);
            renderRevenueTrendChart(analytics);
            renderOccupancyChart();
        } catch (RuntimeException e) {
            applyAnalyticsFallback();
        }
    }

    private void applyAnalyticsFallback() {
        totalRevenueValueLabel.setText("$0.00");
        totalReservationsValueLabel.setText("0");
        mostBookedHotelValueLabel.setText("N/A");
        averageOccupancyValueLabel.setText("0.0%");
        monthlyReservationTrendChart.getData().clear();
        monthlyRevenueTrendChart.getData().clear();
        occupancyRatePieChart.getData().clear();
        occupancySummaryLabel.setText("Occupancy data unavailable.");
    }

    private void renderReservationTrendChart(ManagerAnalyticsModel analytics) {
        monthlyReservationTrendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (analytics.monthlyTrend() != null) {
            analytics.monthlyTrend().forEach(point ->
                    series.getData().add(new XYChart.Data<>(safeDisplay(point.monthLabel(), "N/A"), Math.max(0, point.reservationCount())))
            );
        }

        monthlyReservationTrendChart.getData().add(series);

        Platform.runLater(() -> series.getData().forEach(data -> {
            if (data.getNode() == null) {
                return;
            }
            Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + ": " + data.getYValue().intValue() + " reservations"));
        }));
    }

    private void renderRevenueTrendChart(ManagerAnalyticsModel analytics) {
        monthlyRevenueTrendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (analytics.revenueTrend() != null) {
            analytics.revenueTrend().forEach(point -> {
                BigDecimal totalRevenue = point.totalRevenue() == null ? BigDecimal.ZERO : point.totalRevenue();
                series.getData().add(new XYChart.Data<>(safeDisplay(point.monthLabel(), "N/A"), totalRevenue));
            });
        }

        monthlyRevenueTrendChart.getData().add(series);

        Platform.runLater(() -> series.getData().forEach(data -> {
            if (data.getNode() == null) {
                return;
            }
            BigDecimal revenue = new BigDecimal(data.getYValue().toString());
            Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + ": " + formatCurrency(revenue)));
        }));
    }

    private void renderOccupancyChart() {
        int totalRooms = allHotels.stream().mapToInt(hotel -> Math.max(0, hotel.totalRooms())).sum();
        int availableRooms = allHotels.stream()
                .mapToInt(hotel -> Math.max(0, Math.min(hotel.totalRooms(), hotel.availableRooms())))
                .sum();
        int occupiedRooms = Math.max(0, totalRooms - availableRooms);

        if (totalRooms <= 0) {
            occupancyRatePieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("No occupancy data", 1)
            ));
            occupancySummaryLabel.setText("Occupied 0 of 0 rooms (0.0%).");
            installPieChartTooltips();
            return;
        }

        occupancyRatePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Occupied", occupiedRooms),
                new PieChart.Data("Available", availableRooms)
        ));

        double occupancyPercent = Math.min(100.0, Math.max(0.0, (occupiedRooms * 100.0) / totalRooms));
        occupancySummaryLabel.setText(String.format(
                Locale.US,
                "Occupied %d of %d rooms (%.1f%%).",
                occupiedRooms,
                totalRooms,
                occupancyPercent
        ));

        installPieChartTooltips();
    }

    private void installPieChartTooltips() {
        Platform.runLater(() -> occupancyRatePieChart.getData().forEach(data -> {
            if (data.getNode() == null) {
                return;
            }
            String valueLabel = Math.round(data.getPieValue()) + " rooms";
            Tooltip.install(data.getNode(), new Tooltip(data.getName() + ": " + valueLabel));
        }));
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount;
        return CURRENCY_FORMAT.format(normalized);
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> refreshAnalyticsData()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        if (rootPane == null || rootPane.getScene() == null) {
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Unable to open requested view.");
        }
    }

    private void redirectToRoleSelection() {
        stopAutoRefresh();
        if (rootPane == null) {
            return;
        }
        if (rootPane.getScene() == null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    redirectToRoleSelection();
                }
            });
            return;
        }
        navigateTo("/RoleSelection.fxml", "FurHope - Access Portal");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeDisplay(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}

