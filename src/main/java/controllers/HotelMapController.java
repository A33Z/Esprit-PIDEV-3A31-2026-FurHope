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

import java.io.IOException;
import java.util.List;

public class HotelMapController {

    @FXML
    private Label mapTitleLabel;
    @FXML
    private Label mapMessageLabel;
    @FXML
    private WebView mapWebView;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapBridge mapBridge = new MapBridge();

    public void initializeMap(String city, List<HotelMapMarkerModel> markers) {
        mapTitleLabel.setText("Map - " + city);
        if (markers == null || markers.isEmpty()) {
            mapMessageLabel.setText("No map-ready coordinates available for this search.");
        } else {
            mapMessageLabel.setText(markers.size() + " hotels mapped.");
        }

        WebEngine engine = mapWebView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", mapBridge);
            injectMarkers(engine, markers);
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

    private void injectMarkers(WebEngine engine, List<HotelMapMarkerModel> markers) {
        try {
            String markersJson = objectMapper.writeValueAsString(markers == null ? List.of() : markers);
            engine.executeScript("loadMarkers(" + markersJson + ");");
        } catch (JsonProcessingException e) {
            mapMessageLabel.setText("Could not render map markers.");
        }
    }

    private String buildHtml(String city) {
        String safeCity = city == null ? "Hotels" : city.replace("'", "");
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1"/>
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                    <style>
                        html, body, #map { height: 100%%; margin: 0; padding: 0; }
                        body { font-family: Arial, sans-serif; }
                        .popup-title { font-weight: 700; margin-bottom: 4px; color: #17352d; }
                        .popup-rating { color: #1f5f4a; font-size: 12px; margin-bottom: 4px; }
                        .popup-preview { font-size: 12px; margin-bottom: 8px; max-width: 220px; }
                        .popup-button {
                            background: #ff6b4a;
                            color: #fff;
                            border: none;
                            border-radius: 8px;
                            padding: 6px 10px;
                            cursor: pointer;
                            font-weight: 600;
                        }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <script>
                        const map = L.map('map').setView([40.7128, -74.0060], 11);
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
                            div.style.color = '#17352d';
                            div.innerText = '%s';
                            return div;
                        };
                        titleControl.addTo(map);

                        function escapeHtml(text) {
                            return String(text || '')
                                .replace(/&/g, '&amp;')
                                .replace(/</g, '&lt;')
                                .replace(/>/g, '&gt;')
                                .replace(/\"/g, '&quot;');
                        }

                        function openDetails(hotelId) {
                            if (window.javaBridge && window.javaBridge.openHotelDetails) {
                                window.javaBridge.openHotelDetails(hotelId);
                            }
                        }

                        function loadMarkers(markers) {
                            if (!Array.isArray(markers) || markers.length === 0) {
                                return;
                            }
                            const bounds = [];
                            markers.forEach(marker => {
                                if (Number.isNaN(marker.latitude) || Number.isNaN(marker.longitude)) {
                                    return;
                                }
                                const popup = `
                                    <div class=\"popup-title\">${escapeHtml(marker.name)}</div>
                                    <div class=\"popup-rating\">Rating: ${Number(marker.rating).toFixed(1)} / 5.0</div>
                                    <div class=\"popup-preview\">${escapeHtml(marker.preview || '')}</div>
                                    <button class=\"popup-button\" onclick=\"openDetails(${marker.hotelId})\">View Details</button>
                                `;
                                L.marker([marker.latitude, marker.longitude]).addTo(map).bindPopup(popup);
                                bounds.push([marker.latitude, marker.longitude]);
                            });
                            if (bounds.length > 0) {
                                map.fitBounds(bounds, { padding: [25, 25] });
                            }
                        }
                    </script>
                </body>
                </html>
                """.formatted(safeCity);
    }

    public class MapBridge {
        public void openHotelDetails(int hotelId) {
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
    }
}
