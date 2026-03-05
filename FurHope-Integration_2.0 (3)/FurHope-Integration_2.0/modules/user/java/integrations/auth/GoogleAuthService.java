package integrations.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class GoogleAuthService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String state;

    public record GoogleProfile(String email, String givenName, String familyName) {}

    public GoogleAuthService() {
        Properties app = loadAppProperties();
        this.clientId = firstNonBlank(System.getenv("GOOGLE_OAUTH_CLIENT_ID"), app.getProperty("google.oauth.client_id"));
        this.clientSecret = firstNonBlank(System.getenv("GOOGLE_OAUTH_CLIENT_SECRET"), app.getProperty("google.oauth.client_secret"));
        this.redirectUri = firstNonBlank(System.getenv("GOOGLE_OAUTH_REDIRECT_URI"),
                app.getProperty("google.oauth.redirect_uri"),
                "http://localhost:8765/oauth2/callback");
        this.state = UUID.randomUUID().toString();
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }

    public GoogleProfile authenticate() {
        if (!isConfigured()) {
            throw new RuntimeException("Google OAuth is not configured.");
        }
        URI redirect = URI.create(redirectUri);
        int port = redirect.getPort() > 0 ? redirect.getPort() : 8765;
        String callbackPath = redirect.getPath() == null || redirect.getPath().isBlank()
                ? "/oauth2/callback"
                : redirect.getPath();

        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            HttpServer finalServer = server;
            server.createContext(callbackPath, exchange -> {
                String query = exchange.getRequestURI().getRawQuery();
                Map<String, String> params = parseQuery(query);
                String error = params.get("error");
                String code = params.get("code");
                String callbackState = params.get("state");
                String body;
                if (error != null && !error.isBlank()) {
                    codeFuture.completeExceptionally(new RuntimeException("Google returned error: " + error));
                    body = "<html><body><h3>Login cancelled or failed. Return to the app.</h3></body></html>";
                } else if (callbackState == null || !callbackState.equals(state)) {
                    codeFuture.completeExceptionally(new RuntimeException("Invalid OAuth state."));
                    body = "<html><body><h3>Invalid auth state. Return to the app.</h3></body></html>";
                } else if (code != null && !code.isBlank()) {
                    codeFuture.complete(code);
                    body = "<html><body><h3>Login successful. You can close this tab.</h3></body></html>";
                } else {
                    codeFuture.completeExceptionally(new RuntimeException("Google sign-in did not return an authorization code."));
                    body = "<html><body><h3>Login failed. Please return to the app.</h3></body></html>";
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                finalServer.stop(0);
            });
            server.start();

            String authUrl = AUTH_ENDPOINT
                    + "?client_id=" + url(clientId)
                    + "&redirect_uri=" + url(redirectUri)
                    + "&response_type=code"
                    + "&scope=" + url("openid email profile")
                    + "&access_type=offline"
                    + "&prompt=select_account"
                    + "&state=" + url(state);

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new RuntimeException("Desktop browser is not supported. Open this URL manually: " + authUrl);
            }
            Desktop.getDesktop().browse(URI.create(authUrl));

            String code = codeFuture.get(120, TimeUnit.SECONDS);
            String accessToken = exchangeCodeForAccessToken(code);
            return fetchUserProfile(accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Google sign-in failed: " + e.getMessage(), e);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private String exchangeCodeForAccessToken(String code) throws IOException, InterruptedException {
        String body = "code=" + url(code)
                + "&client_id=" + url(clientId)
                + "&client_secret=" + url(clientSecret)
                + "&redirect_uri=" + url(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Token exchange failed with HTTP " + response.statusCode());
        }
        JsonNode json = MAPPER.readTree(response.body());
        String token = json.path("access_token").asText("");
        if (token.isBlank()) {
            throw new RuntimeException("Google token response does not contain access_token.");
        }
        return token;
    }

    private GoogleProfile fetchUserProfile(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Failed to fetch Google user profile (HTTP " + response.statusCode() + ").");
        }
        JsonNode json = MAPPER.readTree(response.body());
        String email = json.path("email").asText("");
        if (email.isBlank()) {
            throw new RuntimeException("Google profile does not contain email.");
        }
        String givenName = json.path("given_name").asText("User");
        String familyName = json.path("family_name").asText("Google");
        return new GoogleProfile(email, givenName, familyName);
    }

    private Properties loadAppProperties() {
        Properties props = new Properties();
        try (var in = GoogleAuthService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
        }
        loadOptionalFile(props, "infrastructure/secrets/google.secrets.properties");
        loadOptionalFile(props, "google.secrets.properties"); // legacy fallback
        return props;
    }

    private void loadOptionalFile(Properties properties, String path) {
        try {
            File file = new File(path);
            if (file.isFile()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        String[] pairs = query.split("&");
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = decode(kv[0]);
            String val = kv.length > 1 ? decode(kv[1]) : "";
            map.put(key, val);
        }
        return map;
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
