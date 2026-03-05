package com.projet.payment.service;

import com.projet.payment.dto.PaymentConfirmRequest;
import com.projet.payment.dto.PaymentRequest;
import com.projet.payment.dto.PaymentResponse;
import com.projet.payment.model.CardPayment;
import com.projet.payment.model.Payment;
import com.projet.payment.model.PaypalPayment;
import com.projet.payment.model.StripePayment;
import com.projet.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";
    private static final String PENDING_STATUS = "PENDING";

    private final PaymentRepository paymentRepository;
    private final PaymentEmailService paymentEmailService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String stripeSecretKey;
    private final String stripePublishableKey;
    private final String stripeCurrency;
    private final String stripeSuccessUrl;
    private final String stripeCancelUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEmailService paymentEmailService,
            @Value("${stripe.secret-key:}") String stripeSecretKey,
            @Value("${stripe.publishable-key:}") String stripePublishableKey,
            @Value("${stripe.currency:usd}") String stripeCurrency,
            @Value("${stripe.success-url:https://example.com/furhope/payment-success?session_id={CHECKOUT_SESSION_ID}}") String stripeSuccessUrl,
            @Value("${stripe.cancel-url:https://example.com/furhope/payment-cancelled}") String stripeCancelUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEmailService = paymentEmailService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newHttpClient();
        this.stripeSecretKey = stripeSecretKey == null ? "" : stripeSecretKey.trim();
        this.stripePublishableKey = stripePublishableKey == null ? "" : stripePublishableKey.trim();
        this.stripeCurrency = stripeCurrency == null ? "usd" : stripeCurrency.trim();
        this.stripeSuccessUrl = stripeSuccessUrl == null ? "" : stripeSuccessUrl.trim();
        this.stripeCancelUrl = stripeCancelUrl == null ? "" : stripeCancelUrl.trim();
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request.getPaymentMethod() != null && request.getPaymentMethod().name().equalsIgnoreCase("STRIPE")) {
            return processStripePayment(request);
        }

        String transactionId = UUID.randomUUID().toString();
        String initialStatus = determinePreCheckStatus(request);
        String confirmationCode = generateConfirmationCode();

        Payment payment = switch (request.getPaymentMethod()) {
            case CARD -> new CardPayment(request.getCustomerName(), request.getCustomerEmail(), request.getAmount(), transactionId, initialStatus);
            case PAYPAL -> new PaypalPayment(request.getCustomerName(), request.getCustomerEmail(), request.getAmount(), transactionId, initialStatus);
            default -> throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
        };

        payment.setConfirmationCode(confirmationCode);
        payment.setConfirmationExpiresAt(Instant.now().plusSeconds(600));

        Payment savedPayment = paymentRepository.save(payment);
        if (FAILED_STATUS.equals(savedPayment.getStatus())) {
            return new PaymentResponse(savedPayment.getStatus(), savedPayment.getTransactionId(), "Payment pre-check failed.");
        }

        try {
            paymentEmailService.sendConfirmationCode(savedPayment.getCustomerEmail(), savedPayment.getTransactionId(), confirmationCode);
            return new PaymentResponse(savedPayment.getStatus(), savedPayment.getTransactionId(), "Verification code sent by email.");
        } catch (Exception ex) {
            savedPayment.setStatus(FAILED_STATUS);
            paymentRepository.save(savedPayment);
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new PaymentResponse(FAILED_STATUS, savedPayment.getTransactionId(), "Unable to send confirmation email: " + detail);
        }
    }

    @Transactional
    public PaymentResponse verifyStripePayment(String transactionId) {
        String normalizedTransactionId = transactionId == null ? "" : transactionId.trim();
        if (normalizedTransactionId.isEmpty()) {
            return new PaymentResponse(FAILED_STATUS, "N/A", "Transaction ID is required.");
        }

        Payment payment = paymentRepository.findByTransactionId(normalizedTransactionId)
                .orElse(null);
        if (payment == null) {
            return new PaymentResponse(FAILED_STATUS, normalizedTransactionId, "Transaction not found.");
        }

        if (!(payment instanceof StripePayment)) {
            return new PaymentResponse(FAILED_STATUS, normalizedTransactionId, "Transaction is not a Stripe checkout.");
        }

        if (SUCCESS_STATUS.equalsIgnoreCase(payment.getStatus())) {
            return new PaymentResponse(SUCCESS_STATUS, normalizedTransactionId, "Payment already confirmed.");
        }

        if (FAILED_STATUS.equalsIgnoreCase(payment.getStatus())) {
            return new PaymentResponse(FAILED_STATUS, normalizedTransactionId, "Payment already failed.");
        }

        if (!isStripeSecretConfigured()) {
            return new PaymentResponse(
                    FAILED_STATUS,
                    normalizedTransactionId,
                    "Stripe secret key is missing. Set STRIPE_SECRET_KEY or stripe.secret-key."
            );
        }

        String checkoutSessionId = payment.getProviderPaymentId();
        if (checkoutSessionId == null || checkoutSessionId.isBlank()) {
            payment.setStatus(FAILED_STATUS);
            paymentRepository.save(payment);
            return new PaymentResponse(FAILED_STATUS, normalizedTransactionId, "Stripe checkout session id is missing.");
        }

        try {
            StripeSessionStatus stripeStatus = fetchStripeSessionStatus(checkoutSessionId);
            if ("paid".equalsIgnoreCase(stripeStatus.paymentStatus())) {
                payment.setStatus(SUCCESS_STATUS);
                payment.setConfirmationCode(null);
                payment.setConfirmationExpiresAt(null);
                paymentRepository.save(payment);
                String emailMessage = sendFinalConfirmationEmail(payment);
                return new PaymentResponse(SUCCESS_STATUS, normalizedTransactionId, "Stripe payment confirmed. " + emailMessage);
            }

            if ("expired".equalsIgnoreCase(stripeStatus.checkoutStatus())) {
                payment.setStatus(FAILED_STATUS);
                paymentRepository.save(payment);
                return new PaymentResponse(FAILED_STATUS, normalizedTransactionId, "Stripe checkout session expired.");
            }

            return new PaymentResponse(
                    PENDING_STATUS,
                    normalizedTransactionId,
                    "Stripe payment is still pending. Complete payment in checkout and verify again."
            );
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new PaymentResponse(PENDING_STATUS, normalizedTransactionId, "Unable to verify Stripe payment right now: " + detail);
        }
    }

    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (payment instanceof StripePayment stripePayment) {
            return confirmStripeCheckout(stripePayment, request.getConfirmationCode());
        }

        if (FAILED_STATUS.equals(payment.getStatus())) {
            return new PaymentResponse(FAILED_STATUS, payment.getTransactionId(), "Payment already failed.");
        }

        if (SUCCESS_STATUS.equals(payment.getStatus())) {
            return new PaymentResponse(SUCCESS_STATUS, payment.getTransactionId(), "Payment already confirmed.");
        }

        if (payment.getConfirmationExpiresAt() == null || Instant.now().isAfter(payment.getConfirmationExpiresAt())) {
            payment.setStatus(FAILED_STATUS);
            paymentRepository.save(payment);
            return new PaymentResponse(FAILED_STATUS, payment.getTransactionId(), "Confirmation code expired.");
        }

        if (!request.getConfirmationCode().equals(payment.getConfirmationCode())) {
            return new PaymentResponse(PENDING_STATUS, payment.getTransactionId(), "Invalid confirmation code.");
        }

        payment.setStatus(SUCCESS_STATUS);
        payment.setConfirmationCode(null);
        payment.setConfirmationExpiresAt(null);
        paymentRepository.save(payment);

        String emailMessage = sendFinalConfirmationEmail(payment);
        return new PaymentResponse(SUCCESS_STATUS, payment.getTransactionId(), "Payment confirmed. " + emailMessage);
    }

    private PaymentResponse processStripePayment(PaymentRequest request) {
        String transactionId = UUID.randomUUID().toString();

        if (!isStripeSecretConfigured()) {
            return new PaymentResponse(
                    FAILED_STATUS,
                    transactionId,
                    "Stripe secret key is missing. Set STRIPE_SECRET_KEY or stripe.secret-key."
            );
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentResponse(FAILED_STATUS, transactionId, "Amount must be greater than zero.");
        }

        StripePayment payment = new StripePayment(
                request.getCustomerName(),
                request.getCustomerEmail(),
                request.getAmount(),
                transactionId,
                PENDING_STATUS
        );
        payment.setProviderName("STRIPE");
        String confirmationCode = generateConfirmationCode();
        payment.setConfirmationCode(confirmationCode);
        payment.setConfirmationExpiresAt(Instant.now().plusSeconds(600));

        StripePayment savedPayment = (StripePayment) paymentRepository.save(payment);
        try {
            paymentEmailService.sendConfirmationCode(savedPayment.getCustomerEmail(), savedPayment.getTransactionId(), confirmationCode);
            return new PaymentResponse(
                    PENDING_STATUS,
                    savedPayment.getTransactionId(),
                    "Verification code sent by email. Enter the code to continue to Stripe checkout."
            );
        } catch (Exception ex) {
            savedPayment.setStatus(FAILED_STATUS);
            paymentRepository.save(savedPayment);
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new PaymentResponse(FAILED_STATUS, savedPayment.getTransactionId(), "Unable to send confirmation email: " + detail);
        }
    }

    @Transactional
    private PaymentResponse confirmStripeCheckout(StripePayment payment, String confirmationCode) {
        if (FAILED_STATUS.equals(payment.getStatus())) {
            return new PaymentResponse(FAILED_STATUS, payment.getTransactionId(), "Payment already failed.");
        }

        if (SUCCESS_STATUS.equals(payment.getStatus())) {
            return new PaymentResponse(SUCCESS_STATUS, payment.getTransactionId(), "Payment already confirmed.");
        }

        if (payment.getConfirmationExpiresAt() == null || Instant.now().isAfter(payment.getConfirmationExpiresAt())) {
            payment.setStatus(FAILED_STATUS);
            paymentRepository.save(payment);
            return new PaymentResponse(FAILED_STATUS, payment.getTransactionId(), "Confirmation code expired. Restart checkout.");
        }

        if (confirmationCode == null || !confirmationCode.equals(payment.getConfirmationCode())) {
            return new PaymentResponse(PENDING_STATUS, payment.getTransactionId(), "Invalid confirmation code.");
        }

        try {
            StripeCheckoutSession checkoutSession = createStripeCheckoutSession(payment);
            payment.setProviderName("STRIPE");
            payment.setProviderPaymentId(checkoutSession.sessionId());
            payment.setConfirmationCode(null);
            payment.setConfirmationExpiresAt(null);
            payment.setStatus(PENDING_STATUS);
            paymentRepository.save(payment);

            String message = stripePublishableKey.isBlank()
                    ? "Verification successful. Complete payment in Stripe checkout."
                    : "Verification successful. Complete payment in Stripe checkout. Publishable key configured.";
            return new PaymentResponse(PENDING_STATUS, payment.getTransactionId(), message, checkoutSession.checkoutUrl());
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new PaymentResponse(FAILED_STATUS, payment.getTransactionId(), "Unable to create Stripe checkout session: " + detail);
        }
    }

    private String determinePreCheckStatus(PaymentRequest request) {
        simulateProcessingLatency();

        BigDecimal amount = request.getAmount();
        switch (request.getPaymentMethod()) {
            case CARD -> {
                if (amount.compareTo(BigDecimal.valueOf(1200)) > 0) {
                    return FAILED_STATUS;
                }
            }
            case PAYPAL -> {
                if (amount.compareTo(BigDecimal.valueOf(2000)) > 0) {
                    return FAILED_STATUS;
                }
            }
            case STRIPE -> {
                return PENDING_STATUS;
            }
            default -> {
                return FAILED_STATUS;
            }
        }
        return PENDING_STATUS;
    }

    private void simulateProcessingLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(700, 1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateConfirmationCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private String sendFinalConfirmationEmail(Payment payment) {
        if (payment == null) {
            return "Confirmation email not sent: payment data missing.";
        }

        String code = generateConfirmationCode();
        try {
            paymentEmailService.sendPaymentSuccessConfirmation(
                    payment.getCustomerEmail(),
                    payment.getTransactionId(),
                    code,
                    payment.getAmount()
            );
            return "Confirmation email sent with code " + code + ".";
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return "Payment is confirmed, but confirmation email failed: " + detail;
        }
    }

    private boolean isStripeSecretConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private StripeCheckoutSession createStripeCheckoutSession(Payment payment) throws IOException, InterruptedException {
        BigDecimal normalizedAmount = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        long amountInMinor = normalizedAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

        List<String> encodedParams = new ArrayList<>();
        encodedParams.add(encodeParam("mode", "payment"));
        encodedParams.add(encodeParam("success_url", resolveStripeSuccessUrl()));
        encodedParams.add(encodeParam("cancel_url", resolveStripeCancelUrl()));
        encodedParams.add(encodeParam("customer_email", payment.getCustomerEmail()));
        encodedParams.add(encodeParam("client_reference_id", payment.getTransactionId()));
        encodedParams.add(encodeParam("line_items[0][price_data][currency]", resolveStripeCurrency()));
        encodedParams.add(encodeParam("line_items[0][price_data][unit_amount]", String.valueOf(amountInMinor)));
        encodedParams.add(encodeParam("line_items[0][price_data][product_data][name]", "FurHope Cart Checkout"));
        encodedParams.add(encodeParam("line_items[0][quantity]", "1"));
        encodedParams.add(encodeParam("metadata[transaction_id]", payment.getTransactionId()));
        String requestBody = String.join("&", encodedParams);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.stripe.com/v1/checkout/sessions"))
                .header("Authorization", "Bearer " + stripeSecretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Stripe API returned HTTP " + response.statusCode() + ": " + extractStripeErrorMessage(response.body()));
        }

        JsonNode payload = objectMapper.readTree(response.body());
        String sessionId = asText(payload, "id");
        String checkoutUrl = asText(payload, "url");
        if (sessionId.isBlank() || checkoutUrl.isBlank()) {
            throw new IOException("Stripe API response missing checkout session fields.");
        }

        return new StripeCheckoutSession(sessionId, checkoutUrl);
    }

    private StripeSessionStatus fetchStripeSessionStatus(String checkoutSessionId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.stripe.com/v1/checkout/sessions/" + encodePath(checkoutSessionId)))
                .header("Authorization", "Bearer " + stripeSecretKey)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Stripe API returned HTTP " + response.statusCode() + ": " + extractStripeErrorMessage(response.body()));
        }

        JsonNode payload = objectMapper.readTree(response.body());
        return new StripeSessionStatus(
                asText(payload, "status"),
                asText(payload, "payment_status")
        );
    }

    private String extractStripeErrorMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            return "empty response";
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode errorNode = root.path("error");
            String message = asText(errorNode, "message");
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // Fallback to raw payload below.
        }
        return payload;
    }

    private String asText(JsonNode node, String fieldName) {
        if (node == null) {
            return "";
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return "";
        }
        return valueNode.asText("");
    }

    private String encodeParam(String key, String value) {
        String safeValue = value == null ? "" : value;
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(safeValue, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String resolveStripeSuccessUrl() {
        if (stripeSuccessUrl == null || stripeSuccessUrl.isBlank()) {
            return "https://example.com/furhope/payment-success?session_id={CHECKOUT_SESSION_ID}";
        }
        return stripeSuccessUrl;
    }

    private String resolveStripeCancelUrl() {
        if (stripeCancelUrl == null || stripeCancelUrl.isBlank()) {
            return "https://example.com/furhope/payment-cancelled";
        }
        return stripeCancelUrl;
    }

    private String resolveStripeCurrency() {
        String normalizedCurrency = stripeCurrency == null ? "" : stripeCurrency.trim().toLowerCase();
        return normalizedCurrency.isEmpty() ? "usd" : normalizedCurrency;
    }

    private record StripeCheckoutSession(String sessionId, String checkoutUrl) {
    }

    private record StripeSessionStatus(String checkoutStatus, String paymentStatus) {
    }
}
