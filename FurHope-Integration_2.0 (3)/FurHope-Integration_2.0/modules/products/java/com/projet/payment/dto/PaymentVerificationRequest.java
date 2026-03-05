package com.projet.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentVerificationRequest {

    @NotBlank
    private String transactionId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
