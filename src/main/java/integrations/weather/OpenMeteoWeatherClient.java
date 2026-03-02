package integrations.weather;

import com.fasterxml.jackson.databind.JsonNode;
import integrations.http.ExternalApiException;
import integrations.http.HttpJsonClient;

public class OpenMeteoWeatherClient {

    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";

    private final HttpJsonClient httpJsonClient;

    public OpenMeteoWeatherClient() {
        this.httpJsonClient = new HttpJsonClient();
    }

    public String getCurrentWeatherSummary(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return "Weather unavailable";
        }

        String url = ENDPOINT
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,weather_code"
                + "&timezone=auto";
        try {
            JsonNode payload = httpJsonClient.getJson(url);
            JsonNode current = payload.path("current");
            if (!current.isObject()) {
                return "Weather unavailable";
            }

            if (!current.hasNonNull("temperature_2m")) {
                return "Weather unavailable";
            }

            double temperature = current.path("temperature_2m").asDouble(Double.NaN);
            int weatherCode = current.path("weather_code").asInt(-1);

            if (Double.isNaN(temperature)) {
                return "Weather unavailable";
            }

            return String.format("%.1f C, %s", temperature, decodeWeatherCode(weatherCode));
        } catch (ExternalApiException e) {
            return "Weather unavailable";
        }
    }

    private String decodeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown conditions";
        };
    }
}
