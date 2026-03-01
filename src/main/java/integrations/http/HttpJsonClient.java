package integrations.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.esprit.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpJsonClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(18);

    private final HttpClient httpClient;

    public HttpJsonClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public JsonNode getJson(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", AppConfig.httpUserAgent())
                .build();
        return send(request);
    }

    public JsonNode postTextForJson(String url, String contentType, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Accept", "application/json")
                .header("Content-Type", contentType)
                .header("User-Agent", AppConfig.httpUserAgent())
                .build();
        return send(request);
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ExternalApiException("External API call failed with HTTP " + statusCode + ".");
            }
            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException e) {
            throw new ExternalApiException("External API response is not readable.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("External API request was interrupted.", e);
        }
    }
}

