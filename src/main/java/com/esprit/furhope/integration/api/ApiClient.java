package com.esprit.furhope.integration.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public ApiClient() {
        this(ApiConfig.baseUrl());
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? ApiConfig.baseUrl() : baseUrl.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T get(String path, Map<String, ?> queryParams, Class<T> responseType) throws IOException {
        return execute("GET", path, queryParams, null, responseType, null);
    }

    public <T> T get(String path, Map<String, ?> queryParams, TypeReference<T> responseType) throws IOException {
        return execute("GET", path, queryParams, null, null, responseType);
    }

    public <T> T post(String path, Map<String, ?> queryParams, Object body, Class<T> responseType) throws IOException {
        return execute("POST", path, queryParams, body, responseType, null);
    }

    public <T> T post(String path, Map<String, ?> queryParams, Object body, TypeReference<T> responseType) throws IOException {
        return execute("POST", path, queryParams, body, null, responseType);
    }

    public <T> T put(String path, Map<String, ?> queryParams, Object body, Class<T> responseType) throws IOException {
        return execute("PUT", path, queryParams, body, responseType, null);
    }

    public <T> T put(String path, Map<String, ?> queryParams, Object body, TypeReference<T> responseType) throws IOException {
        return execute("PUT", path, queryParams, body, null, responseType);
    }

    public <T> T delete(String path, Map<String, ?> queryParams, Class<T> responseType) throws IOException {
        return execute("DELETE", path, queryParams, null, responseType, null);
    }

    public <T> T delete(String path, Map<String, ?> queryParams, TypeReference<T> responseType) throws IOException {
        return execute("DELETE", path, queryParams, null, null, responseType);
    }

    private <T> T execute(
            String method,
            String path,
            Map<String, ?> queryParams,
            Object body,
            Class<T> classType,
            TypeReference<T> typeReference
    ) throws IOException {
        String url = buildUrl(path, queryParams);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        switch (method) {
            case "GET" -> requestBuilder.GET();
            case "DELETE" -> requestBuilder.DELETE();
            case "POST", "PUT" -> {
                String jsonBody;
                try {
                    jsonBody = mapper.writeValueAsString(body == null ? Map.of() : body);
                } catch (JsonProcessingException e) {
                    throw new IOException("Failed to serialize request body", e);
                }
                requestBuilder.header("Content-Type", "application/json");
                if ("POST".equals(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                } else {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                }
            }
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("API call interrupted", e);
        }

        int status = response.statusCode();
        String responseBody = response.body();
        if (status < 200 || status >= 300) {
            throw new IOException("API " + method + " " + path + " failed with HTTP " + status + " body=" + responseBody);
        }

        if (classType == null && typeReference == null) {
            return null;
        }
        if (classType == Void.class) {
            return null;
        }
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            if (classType != null) {
                return mapper.readValue(responseBody, classType);
            }
            return mapper.readValue(responseBody, typeReference);
        } catch (IOException e) {
            throw new IOException("Failed to parse API response for " + method + " " + path + ": " + responseBody, e);
        }
    }

    private String buildUrl(String path, Map<String, ?> queryParams) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        StringBuilder sb = new StringBuilder(baseUrl).append(normalizedPath);

        if (queryParams != null && !queryParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                if (entry.getValue() == null) continue;
                joiner.add(encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())));
            }
            String query = joiner.toString();
            if (!query.isEmpty()) {
                sb.append("?").append(query);
            }
        }

        return sb.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
