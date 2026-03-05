package com.projet.payment.controller;

import com.projet.payment.dto.PaymentRequest;
import com.projet.payment.dto.PaymentResponse;
import com.projet.payment.dto.PaymentConfirmRequest;
import com.projet.payment.dto.PaymentVerificationRequest;
import com.projet.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-stripe")
    public ResponseEntity<PaymentResponse> verifyStripePayment(@Valid @RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentService.verifyStripePayment(request.getTransactionId());
        return ResponseEntity.ok(response);
    }
}
