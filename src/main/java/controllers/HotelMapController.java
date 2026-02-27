package controllers;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.util.List;
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

    public void initializeMap(String city, List<HotelMapMarkerModel> markers) {
        mapTitleLabel.setText("Map - " + city);
        if (markers == null || markers.isEmpty()) {
            mapMessageLabel.setText("No map-ready coordinates available for this search.");
        } else {
            mapMessageLabel.setText("Select radius and center mode to filter nearby hotels.");
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

        engine.loadContent(buildHtml(city));
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
            if (marker == null) {
                continue;
            }
            String token = "m" + markerIndex++;
            markerTokenToHotelId.put(token, marker.hotelId());
            safeMarkers.add(new PublicMapMarker(
                    token,
                    marker.name(),
                    marker.rating(),
                    normalizePrice(marker.pricePerNight()),
                    marker.latitude(),
                    marker.longitude()
            ));
        }
        return safeMarkers;
    }

    private String normalizePrice(String value) {
        if (value == null || value.isBlank()) {
            return "$85.00 / night";
        }
        return value.trim();
    }

    private void injectMarkers(WebEngine engine, List<PublicMapMarker> markers) {
        try {
            String markersJson = objectMapper.writeValueAsString(markers == null ? List.of() : markers);
            engine.executeScript("loadMarkers(" + markersJson + ");");
        } catch (JsonProcessingException e) {
            mapMessageLabel.setText("Could not render map markers.");
        }
    }

    private String buildHtml(String city) {
        String safeCity = escapeForJavaScriptLiteral(city == null ? "Hotels" : city);
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1"/>
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                    <style>
                        html, body { height: 100%%; margin: 0; padding: 0; overflow: hidden; }
                        body { font-family: "Segoe UI", sans-serif; background: #fffafc; }
                        #map { height: 100%%; width: 100%%; }
                        .map-toolbar {
                            position: absolute;
                            top: 12px;
                            left: 12px;
                            z-index: 1000;
                            background: rgba(255, 255, 255, 0.95);
                            border: 1px solid rgba(227, 96, 154, 0.22);
                            border-radius: 14px;
                            box-shadow: 0 8px 26px rgba(104, 16, 58, 0.14);
                            padding: 10px 12px;
                            display: flex;
                            flex-direction: column;
                            gap: 8px;
                            min-width: 230px;
                        }
                        .toolbar-title {
                            font-size: 13px;
                            font-weight: 800;
                            color: #8b1f58;
                        }
                        .toolbar-row {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                            font-size: 12px;
                            color: #5f3247;
                        }
                        .toolbar-row label {
                            font-weight: 600;
                        }
                        .toolbar-select {
                            border: 1px solid rgba(139, 31, 88, 0.18);
                            border-radius: 8px;
                            padding: 4px 7px;
                            background: #fff;
                            color: #4d2437;
                        }
                        .toolbar-btn {
                            border: none;
                            border-radius: 999px;
                            background: linear-gradient(135deg, #ff5da9, #ff7dbf);
                            color: #fff;
                            padding: 6px 11px;
                            font-size: 12px;
                            font-weight: 700;
                            cursor: pointer;
                        }
                        .toolbar-note {
                            font-size: 11px;
                            color: rgba(95, 50, 71, 0.84);
                        }
                        .pink-marker span {
                            display: block;
                            width: 20px;
                            height: 20px;
                            border-radius: 50%%;
                            background: linear-gradient(145deg, #ff4ea5, #ff80c2);
                            border: 2px solid #ffffff;
                            box-shadow: 0 6px 14px rgba(109, 16, 64, 0.34);
                        }
                        .pink-marker.selected span {
                            animation: markerPulse 0.7s ease-out 1;
                        }
                        @keyframes markerPulse {
                            0%% { transform: scale(1); box-shadow: 0 0 0 rgba(255, 78, 165, 0.5); }
                            40%% { transform: scale(1.28); box-shadow: 0 0 14px rgba(255, 78, 165, 0.58); }
                            100%% { transform: scale(1); box-shadow: 0 0 0 rgba(255, 78, 165, 0.10); }
                        }
                        .popup-shell {
                            min-width: 180px;
                            max-width: 220px;
                        }
                        .popup-title {
                            font-size: 14px;
                            font-weight: 800;
                            color: #7b2050;
                            margin-bottom: 4px;
                        }
                        .popup-price {
                            font-size: 12px;
                            color: #6a2d48;
                            margin-bottom: 3px;
                        }
                        .popup-rating {
                            font-size: 12px;
                            color: #8f3760;
                            margin-bottom: 8px;
                        }
                        .popup-button {
                            width: 100%%;
                            background: linear-gradient(135deg, #ff5aa7, #ff7ac0);
                            color: #fff;
                            border: none;
                            border-radius: 9px;
                            padding: 7px 10px;
                            cursor: pointer;
                            font-weight: 700;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="map-toolbar">
                        <div class="toolbar-title">Radius Filter</div>
                        <div class="toolbar-row">
                            <label for="radiusSelect">Radius</label>
                            <select id="radiusSelect" class="toolbar-select">
                                <option value="1">1 km</option>
                                <option value="5" selected>5 km</option>
                                <option value="10">10 km</option>
                                <option value="20">20 km</option>
                            </select>
                        </div>
                        <div class="toolbar-row">
                            <label><input type="radio" name="centerMode" value="point" checked /> Map point</label>
                            <label><input type="radio" name="centerMode" value="current" /> Current</label>
                        </div>
                        <div class="toolbar-row">
                            <button class="toolbar-btn" id="locateMeBtn">Use Current Location</button>
                        </div>
                        <div class="toolbar-note">Tip: click anywhere on the map to set the point center.</div>
                    </div>
                    <div id="map"></div>
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <script>
                        const map = L.map('map', { zoomControl: true }).setView([40.7128, -74.0060], 11);
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; OpenStreetMap contributors'
                        }).addTo(map);

                        const titleControl = L.control({ position: 'topright' });
                        titleControl.onAdd = function () {
                            const div = L.DomUtil.create('div');
                            div.style.background = 'rgba(255,255,255,0.9)';
                            div.style.padding = '6px 10px';
                            div.style.borderRadius = '10px';
                            div.style.fontWeight = '700';
                            div.style.color = '#7b2050';
                            div.innerText = '%s';
                            return div;
                        };
                        titleControl.addTo(map);

                        const markerLayer = L.layerGroup().addTo(map);
                        const radiusSelect = document.getElementById('radiusSelect');
                        const locateMeBtn = document.getElementById('locateMeBtn');
                        const centerModeRadios = document.querySelectorAll('input[name="centerMode"]');
                        const pinkIcon = L.divIcon({
                            className: 'pink-marker',
                            html: '<span></span>',
                            iconSize: [20, 20],
                            iconAnchor: [10, 10],
                            popupAnchor: [0, -14]
                        });

                        let allMarkers = [];
                        let selectedPointCenter = map.getCenter();
                        let currentLocationCenter = null;
                        let centerPin = null;
                        let radiusCircle = null;
                        let initialBoundsApplied = false;

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

                        function getCenterMode() {
                            const selected = document.querySelector('input[name="centerMode"]:checked');
                            return selected ? selected.value : 'point';
                        }

                        function activeCenter() {
                            if (getCenterMode() === 'current') {
                                return currentLocationCenter;
                            }
                            return selectedPointCenter;
                        }

                        function buildPopup(marker) {
                            const ratingRaw = Number(marker.rating);
                            const ratingLabel = Number.isFinite(ratingRaw) ? ratingRaw.toFixed(1) : 'N/A';
                            return `
                                <div class="popup-shell">
                                    <div class="popup-title">${escapeHtml(marker.name)}</div>
                                    <div class="popup-price">Price: ${escapeHtml(marker.pricePerNight || '$85.00 / night')}</div>
                                    <div class="popup-rating">Rating: ${ratingLabel} / 5.0</div>
                                    <button class="popup-button" onclick="openQuickReservation('${escapeHtml(marker.markerToken)}')">Quick Reservation</button>
                                </div>
                            `;
                        }

                        function openQuickReservation(markerToken) {
                            if (window.javaBridge && window.javaBridge.openQuickReservation) {
                                window.javaBridge.openQuickReservation(markerToken);
                            }
                        }

                        function haversineKm(lat1, lon1, lat2, lon2) {
                            const toRad = (deg) => (deg * Math.PI) / 180;
                            const earthRadiusKm = 6371;
                            const deltaLat = toRad(lat2 - lat1);
                            const deltaLon = toRad(lon2 - lon1);
                            const a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                                + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2))
                                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
                            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                            return earthRadiusKm * c;
                        }

                        function animateSelection(marker) {
                            const iconNode = marker.getElement();
                            if (!iconNode) {
                                return;
                            }
                            iconNode.classList.remove('selected');
                            void iconNode.offsetWidth;
                            iconNode.classList.add('selected');
                        }

                        function updateCenterVisuals(center, radiusKm) {
                            if (!center) {
                                if (centerPin) {
                                    map.removeLayer(centerPin);
                                    centerPin = null;
                                }
                                if (radiusCircle) {
                                    map.removeLayer(radiusCircle);
                                    radiusCircle = null;
                                }
                                return;
                            }
                            if (centerPin) {
                                centerPin.setLatLng(center);
                            } else {
                                centerPin = L.circleMarker(center, {
                                    radius: 5,
                                    color: '#7e2e53',
                                    fillColor: '#ff6db4',
                                    fillOpacity: 0.95,
                                    weight: 2
                                }).addTo(map);
                            }
                            if (radiusCircle) {
                                radiusCircle.setLatLng(center);
                                radiusCircle.setRadius(radiusKm * 1000);
                            } else {
                                radiusCircle = L.circle(center, {
                                    radius: radiusKm * 1000,
                                    color: '#ff6db4',
                                    weight: 2,
                                    fillColor: '#ff96cb',
                                    fillOpacity: 0.12
                                }).addTo(map);
                            }
                        }

                        function applyRadiusFilter() {
                            markerLayer.clearLayers();
                            const radiusKm = Number(radiusSelect.value || '5');
                            const center = activeCenter();

                            if (!center) {
                                updateCenterVisuals(null, radiusKm);
                                syncStatus('Current location unavailable. Click on map or switch to map point mode.');
                                return;
                            }

                            updateCenterVisuals(center, radiusKm);
                            const visibleBounds = [];
                            let visibleCount = 0;

                            for (const marker of allMarkers) {
                                if (!Number.isFinite(Number(marker.latitude)) || !Number.isFinite(Number(marker.longitude))) {
                                    continue;
                                }
                                const distance = haversineKm(center.lat, center.lng, Number(marker.latitude), Number(marker.longitude));
                                if (distance > radiusKm) {
                                    continue;
                                }

                                const leafletMarker = L.marker([marker.latitude, marker.longitude], { icon: pinkIcon }).addTo(markerLayer);
                                leafletMarker.bindPopup(buildPopup(marker));
                                leafletMarker.on('click', () => {
                                    map.flyTo([marker.latitude, marker.longitude], Math.max(13, map.getZoom()), { duration: 0.55 });
                                    animateSelection(leafletMarker);
                                });
                                visibleBounds.push([marker.latitude, marker.longitude]);
                                visibleCount++;
                            }

                            if (!initialBoundsApplied && visibleBounds.length > 0) {
                                map.fitBounds(visibleBounds, { padding: [38, 38], maxZoom: 14 });
                                initialBoundsApplied = true;
                            }

                            const modeLabel = getCenterMode() === 'current' ? 'current location' : 'selected point';
                            syncStatus(`${visibleCount} hotel${visibleCount === 1 ? '' : 's'} within ${radiusKm} km from ${modeLabel}.`);
                        }

                        function requestCurrentLocation(focusMap) {
                            if (!navigator.geolocation) {
                                syncStatus('Geolocation is not supported. Use map point center mode.');
                                return;
                            }
                            navigator.geolocation.getCurrentPosition(
                                (position) => {
                                    currentLocationCenter = L.latLng(position.coords.latitude, position.coords.longitude);
                                    if (getCenterMode() === 'current') {
                                        if (focusMap) {
                                            map.flyTo(currentLocationCenter, 13, { duration: 0.75 });
                                        }
                                        applyRadiusFilter();
                                    } else if (!focusMap) {
                                        applyRadiusFilter();
                                    }
                                },
                                () => {
                                    if (getCenterMode() === 'current') {
                                        syncStatus('Current location unavailable. Click on map to select a center point.');
                                    }
                                },
                                { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
                            );
                        }

                        function loadMarkers(markers) {
                            if (!Array.isArray(markers) || markers.length === 0) {
                                syncStatus('No coordinates available for this city.');
                                return;
                            }
                            allMarkers = markers;
                            const first = markers.find(item => Number.isFinite(Number(item.latitude)) && Number.isFinite(Number(item.longitude)));
                            if (first) {
                                selectedPointCenter = L.latLng(first.latitude, first.longitude);
                                map.setView(selectedPointCenter, 12, { animate: false });
                            }
                            applyRadiusFilter();
                            requestCurrentLocation(false);
                        }

                        map.on('click', (event) => {
                            selectedPointCenter = event.latlng;
                            if (getCenterMode() === 'point') {
                                map.flyTo(selectedPointCenter, Math.max(12, map.getZoom()), { duration: 0.5 });
                            }
                            applyRadiusFilter();
                        });

                        radiusSelect.addEventListener('change', applyRadiusFilter);
                        centerModeRadios.forEach(radio => radio.addEventListener('change', () => {
                            if (radio.value === 'current' && radio.checked) {
                                requestCurrentLocation(true);
                            } else {
                                applyRadiusFilter();
                            }
                        }));
                        locateMeBtn.addEventListener('click', () => requestCurrentLocation(true));
                    </script>
                </body>
                </html>
                """.formatted(safeCity);
    }

    private String escapeForJavaScriptLiteral(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    public class MapBridge {
        public void openQuickReservation(String markerToken) {
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
            double rating,
            String pricePerNight,
            double latitude,
            double longitude
    ) {
    }
}
