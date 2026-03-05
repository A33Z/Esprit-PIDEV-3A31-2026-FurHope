package com.projet.payment.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.payment.OrderApiEndpoints;
import com.projet.payment.dto.OrderCreateRequest;
import com.projet.payment.dto.OrderHistoryResponse;
import com.projet.payment.dto.OrderResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class OrderApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OrderApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public OrderResponse createOrder(String transactionId) throws IOException, InterruptedException {
        String normalizedTransactionId = transactionId == null ? "" : transactionId.trim();
        if (normalizedTransactionId.isEmpty()) {
            return new OrderResponse(false, null, "Transaction ID is required");
        }

        String requestBody = objectMapper.writeValueAsString(new OrderCreateRequest(normalizedTransactionId));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OrderApiEndpoints.getCreateOrderUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Order create API returned HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), OrderResponse.class);
    }

    public List<OrderHistoryResponse> getOrdersByCustomer(String email) throws IOException, InterruptedException {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isEmpty()) {
            return Collections.emptyList();
        }

        String encodedEmail = URLEncoder.encode(normalizedEmail, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OrderApiEndpoints.getCustomerOrdersUrl(encodedEmail)))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Order history API returned HTTP " + response.statusCode());
        }

        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, OrderHistoryResponse.class);
        return objectMapper.readValue(response.body(), listType);
    }
}
