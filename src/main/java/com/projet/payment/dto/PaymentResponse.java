package com.projet.payment.dto;

public class PaymentResponse {

    private final String status;
    private final String transactionId;
    private final String message;

    public PaymentResponse(String status, String transactionId, String message) {
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }
}
