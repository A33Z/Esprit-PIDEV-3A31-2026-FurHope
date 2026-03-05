package controllers;

import application.model.HotelMapDatasetModel;
import application.model.HotelMapMarkerModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HotelMapController {

    @FXML
    private Label mapTitleLabel;
    @FXML
    private Label mapMessageLabel;
    @FXML
    private WebView mapWebView;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapBridge mapBridge = new MapBridge();
    private final Map<String, Integer> markerTokenToHotelId = new HashMap<>();

    public void initializeMap(HotelMapDatasetModel dataset) {
        String city = dataset == null ? "Hotels" : safeDisplay(dataset.city(), "Hotels");
        double defaultLatitude = dataset == null ? 40.7128 : sanitizeLatitude(dataset.defaultLatitude());
        double defaultLongitude = dataset == null ? -74.0060 : sanitizeLongitude(dataset.defaultLongitude());
        int totalHotels = dataset == null ? 0 : Math.max(0, dataset.totalHotels());
        List<HotelMapMarkerModel> markers = dataset == null ? List.of() : dataset.markers();

        mapTitleLabel.setText("Map - " + city);
        if (totalHotels <= 0) {
            mapMessageLabel.setText("No hotels available in database.");
        } else if (markers == null || markers.isEmpty()) {
            mapMessageLabel.setText("Hotels found, but no valid coordinates are available for map rendering.");
        } else {
            mapMessageLabel.setText("Showing " + markers.size() + " hotel pin" + (markers.size() == 1 ? "" : "s") + ".");
        }

        List<PublicMapMarker> publicMarkers = toPublicMarkers(markers);

        WebEngine engine = mapWebView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", mapBridge);
            injectMarkers(engine, publicMarkers);
        });

        engine.loadContent(buildHtml(city, defaultLatitude, defaultLongitude));
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) mapWebView.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private List<PublicMapMarker> toPublicMarkers(List<HotelMapMarkerModel> markers) {
        markerTokenToHotelId.clear();
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }

        List<PublicMapMarker> safeMarkers = new ArrayList<>();
        int markerIndex = 1;
        for (HotelMapMarkerModel marker : markers) {
            if (marker == null || marker.hotelId() <= 0) {
                continue;
            }
            if (!isValidCoordinate(marker.latitude(), marker.longitude())) {
                continue;
            }

            String token = "m" + markerIndex++;
            markerTokenToHotelId.put(token, marker.hotelId());
            safeMarkers.add(new PublicMapMarker(
                    token,
                    sanitizeText(marker.name(), "Hotel", 120),
                    sanitizeText(marker.address(), "Address unavailable", 220),
                    Math.max(0, marker.capacity()),
                    sanitizeText(marker.shortDescription(), "", 220),
                    marker.latitude(),
                    marker.longitude()
            ));
        }
        return safeMarkers;
    }

    private void injectMarkers(WebEngine engine, List<PublicMapMarker> markers) {
        try {
            String markersJson = objectMapper.writeValueAsString(markers == null ? List.of() : markers);
            engine.executeScript("loadMarkers(" + markersJson + ");");
        } catch (JsonProcessingException e) {
            mapMessageLabel.setText("Could not render map markers.");
        }
    }

    private String buildHtml(String city, double defaultLatitude, double defaultLongitude) {
        String safeCity = escapeForJavaScriptLiteral(city == null ? "Hotels" : city);
        String safeDefaultLatitude = String.format(Locale.US, "%.6f", defaultLatitude);
        String safeDefaultLongitude = String.format(Locale.US, "%.6f", defaultLongitude);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset=\"utf-8\"/>
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>
                    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/>
                    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css\"/>
                    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css\"/>
                    <style>
                        html, body { height: 100%%; margin: 0; padding: 0; overflow: hidden; }
                        body { font-family: \"Segoe UI\", sans-serif; background: #f8faf9; }
                        #map { height: 100%%; width: 100%%; }

                        .map-chip {
                            position: absolute;
                            top: 12px;
                            right: 12px;
                            z-index: 1000;
                            background: rgba(255, 255, 255, 0.95);
                            border: 1px solid rgba(22, 52, 44, 0.18);
                            border-radius: 999px;
                            padding: 7px 12px;
                            font-size: 12px;
                            font-weight: 700;
                            color: #1b4b3d;
                            box-shadow: 0 8px 24px rgba(19, 45, 38, 0.15);
                        }

                        .popup-shell { min-width: 220px; max-width: 260px; }
                        .popup-title {
                            font-size: 14px;
                            font-weight: 800;
                            color: #17362d;
                            margin-bottom: 6px;
                        }
                        .popup-line {
                            font-size: 12px;
                            color: rgba(23, 54, 45, 0.9);
                            margin-bottom: 4px;
                            line-height: 1.35;
                        }
                        .popup-desc {
                            font-size: 12px;
                            color: rgba(23, 54, 45, 0.75);
                            margin: 2px 0 10px 0;
                            line-height: 1.35;
                        }
                        .popup-button {
                            width: 100%%;
                            background: linear-gradient(135deg, #ef5959, #d84545);
                            color: #fff;
                            border: none;
                            border-radius: 9px;
                            padding: 7px 10px;
                            cursor: pointer;
                            font-weight: 700;
                            font-size: 12px;
                        }

                        .leaflet-marker-icon.marker-drop {
                            animation: markerDrop 0.35s ease-out;
                        }
                        @keyframes markerDrop {
                            0%% { transform: translateY(-24px); opacity: 0; }
                            100%% { transform: translateY(0); opacity: 1; }
                        }

                        .marker-cluster-small,
                        .marker-cluster-medium,
                        .marker-cluster-large {
                            background: rgba(223, 64, 64, 0.22);
                            border: 1px solid rgba(172, 34, 34, 0.28);
                        }
                        .marker-cluster-small div,
                        .marker-cluster-medium div,
                        .marker-cluster-large div {
                            background: rgba(221, 59, 59, 0.92);
                            color: #fff;
                            font-weight: 800;
                        }
                    </style>
                </head>
                <body>
                    <div class=\"map-chip\">%s</div>
                    <div id=\"map\"></div>

                    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>
                    <script src=\"https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js\"></script>
                    <script>
                        const defaultCenter = [Number(%s), Number(%s)];
                        const map = L.map('map', { zoomControl: true }).setView(defaultCenter, 12);

                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; OpenStreetMap contributors'
                        }).addTo(map);

                        const redPinIcon = L.icon({
                            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
                            shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34],
                            shadowSize: [41, 41]
                        });

                        const markerCluster = L.markerClusterGroup({
                            showCoverageOnHover: false,
                            maxClusterRadius: 52,
                            spiderfyOnMaxZoom: true
                        });
                        map.addLayer(markerCluster);

                        let userCenter = null;
                        let boundsApplied = false;

                        function escapeHtml(text) {
                            return String(text || '')
                                .replace(/&/g, '&amp;')
                                .replace(/</g, '&lt;')
                                .replace(/>/g, '&gt;')
                                .replace(/\"/g, '&quot;');
                        }

                        function syncStatus(message) {
                            if (window.javaBridge && window.javaBridge.updateMapStatus) {
                                window.javaBridge.updateMapStatus(String(message || ''));
                            }
                        }

                        function buildPopup(marker) {
                            const safeCapacity = Number.isFinite(Number(marker.capacity)) && Number(marker.capacity) > 0
                                ? Math.floor(Number(marker.capacity)).toString()
                                : 'N/A';
                            const description = String(marker.shortDescription || '').trim();
                            return `
                                <div class=\"popup-shell\">
                                    <div class=\"popup-title\">${escapeHtml(marker.name)}</div>
                                    <div class=\"popup-line\"><strong>Address:</strong> ${escapeHtml(marker.address || 'Address unavailable')}</div>
                                    <div class=\"popup-line\"><strong>Capacity:</strong> ${escapeHtml(safeCapacity)}</div>
                                    ${description ? `<div class=\"popup-desc\">${escapeHtml(description)}</div>` : ''}
                                    <button class=\"popup-button\" onclick=\"openHotelDetails('${escapeHtml(marker.markerToken)}')\">View Details</button>
                                </div>
                            `;
                        }

                        function openHotelDetails(markerToken) {
                            if (window.javaBridge && window.javaBridge.openHotelDetails) {
                                window.javaBridge.openHotelDetails(markerToken);
                            }
                        }

                        function requestUserLocation() {
                            if (!navigator.geolocation) {
                                return;
                            }
                            navigator.geolocation.getCurrentPosition(
                                (position) => {
                                    userCenter = [position.coords.latitude, position.coords.longitude];
                                    if (!boundsApplied) {
                                        map.setView(userCenter, 12, { animate: true });
                                    }
                                },
                                () => {
                                    if (!boundsApplied) {
                                        map.setView(defaultCenter, 12, { animate: false });
                                    }
                                },
                                { enableHighAccuracy: true, timeout: 8000, maximumAge: 120000 }
                            );
                        }

                        function isValidMarker(marker) {
                            if (!marker || typeof marker !== 'object') {
                                return false;
                            }
                            const lat = Number(marker.latitude);
                            const lng = Number(marker.longitude);
                            if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
                                return false;
                            }
                            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                                return false;
                            }
                            if (typeof marker.markerToken !== 'string' || marker.markerToken.trim() === '') {
                                return false;
                            }
                            return true;
                        }

                        function loadMarkers(markers) {
                            markerCluster.clearLayers();
                            boundsApplied = false;

                            if (!Array.isArray(markers) || markers.length === 0) {
                                syncStatus('No hotel coordinates available.');
                                requestUserLocation();
                                return;
                            }

                            const bounds = [];
                            let validCount = 0;

                            for (const marker of markers) {
                                if (!isValidMarker(marker)) {
                                    continue;
                                }

                                const latitude = Number(marker.latitude);
                                const longitude = Number(marker.longitude);
                                const leafletMarker = L.marker([latitude, longitude], { icon: redPinIcon, riseOnHover: true });
                                leafletMarker.bindPopup(buildPopup(marker));
                                leafletMarker.on('add', () => {
                                    const iconNode = leafletMarker.getElement();
                                    if (!iconNode) {
                                        return;
                                    }
                                    iconNode.classList.remove('marker-drop');
                                    void iconNode.offsetWidth;
                                    iconNode.classList.add('marker-drop');
                                });
                                markerCluster.addLayer(leafletMarker);
                                bounds.push([latitude, longitude]);
                                validCount++;
                            }

                            if (bounds.length > 0) {
                                map.fitBounds(bounds, { padding: [42, 42], maxZoom: 15 });
                                boundsApplied = true;
                            } else if (userCenter) {
                                map.setView(userCenter, 12, { animate: true });
                            } else {
                                map.setView(defaultCenter, 12, { animate: false });
                            }

                            syncStatus(`${validCount} hotel pin${validCount === 1 ? '' : 's'} loaded.`);
                            requestUserLocation();
                        }

                        requestUserLocation();
                    </script>
                </body>
                </html>
                """.formatted(safeCity, safeDefaultLatitude, safeDefaultLongitude);
    }

    private String safeDisplay(String value, String fallback) {
        String normalized = sanitizeText(value, "", 180);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String sanitizeText(String value, String fallback, int maxLength) {
        String normalized = value == null
                ? ""
                : value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private double sanitizeLatitude(double latitude) {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            return 40.7128;
        }
        return latitude;
    }

    private double sanitizeLongitude(double longitude) {
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            return -74.0060;
        }
        return longitude;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private String escapeForJavaScriptLiteral(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    public class MapBridge {
        public void openHotelDetails(String markerToken) {
            Integer hotelId = markerTokenToHotelId.get(markerToken);
            if (hotelId == null || hotelId <= 0) {
                Platform.runLater(() -> mapMessageLabel.setText("Could not resolve selected hotel."));
                return;
            }
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/HotelDetailsView.fxml"));
                    Parent root = loader.load();
                    HotelDetailsController controller = loader.getController();
                    controller.loadHotel(hotelId);

                    Stage stage = new Stage();
                    stage.setTitle("FurHope - Hotel Details");
                    stage.setScene(new Scene(root));
                    stage.setMinWidth(920);
                    stage.setMinHeight(720);
                    stage.show();
                } catch (IOException e) {
                    mapMessageLabel.setText("Could not open hotel details from map.");
                }
            });
        }

        public void updateMapStatus(String statusText) {
            Platform.runLater(() -> mapMessageLabel.setText(statusText == null ? "" : statusText.trim()));
        }
    }

    private record PublicMapMarker(
            String markerToken,
            String name,
            String address,
            int capacity,
            String shortDescription,
            double latitude,
            double longitude
    ) {
    }
}

