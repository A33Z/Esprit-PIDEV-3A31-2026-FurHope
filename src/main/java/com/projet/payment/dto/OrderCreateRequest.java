package com.projet.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderCreateRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    public OrderCreateRequest() {
    }

    public OrderCreateRequest(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
