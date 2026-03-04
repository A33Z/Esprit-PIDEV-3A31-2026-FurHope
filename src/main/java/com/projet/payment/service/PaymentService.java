package com.projet.payment.service;

import com.projet.payment.dto.PaymentConfirmRequest;
import com.projet.payment.dto.PaymentRequest;
import com.projet.payment.dto.PaymentResponse;
import com.projet.payment.model.CardPayment;
import com.projet.payment.model.Payment;
import com.projet.payment.model.PaypalPayment;
import com.projet.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";
    private static final String PENDING_STATUS = "PENDING";

    private final PaymentRepository paymentRepository;
    private final PaymentEmailService paymentEmailService;

    public PaymentService(PaymentRepository paymentRepository, PaymentEmailService paymentEmailService) {
        this.paymentRepository = paymentRepository;
        this.paymentEmailService = paymentEmailService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        String transactionId = UUID.randomUUID().toString();
        String initialStatus = determinePreCheckStatus(request);
        String confirmationCode = generateConfirmationCode();

        Payment payment = switch (request.getPaymentMethod()) {
            case CARD -> new CardPayment(request.getCustomerName(), request.getCustomerEmail(), request.getAmount(), transactionId, initialStatus);
            case PAYPAL -> new PaypalPayment(request.getCustomerName(), request.getCustomerEmail(), request.getAmount(), transactionId, initialStatus);
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
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

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

        return new PaymentResponse(SUCCESS_STATUS, payment.getTransactionId(), "Payment confirmed.");
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
}
