package com.projet.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CurrencyRateService {

    private static final String API_URL_TEMPLATE = "https://api.frankfurter.dev/v1/latest?base=%s&symbols=%s";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public BigDecimal fetchRate(String baseCurrency, String targetCurrency) throws IOException, InterruptedException {
        if (baseCurrency == null || baseCurrency.isBlank() || targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("Currency codes are required.");
        }

        String base = baseCurrency.trim().toUpperCase();
        String target = targetCurrency.trim().toUpperCase();

        if (base.equals(target)) {
            return BigDecimal.ONE;
        }

        String endpoint = String.format(API_URL_TEMPLATE, base, target);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Currency API returned HTTP " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject rates = root.getAsJsonObject("rates");
        if (rates == null || !rates.has(target)) {
            throw new IOException("Currency " + target + " is not available from the API.");
        }

        return rates.get(target).getAsBigDecimal();
    }
}

