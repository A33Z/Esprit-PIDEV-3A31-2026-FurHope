package com.projet.payment.dto;

public class PaymentResponse {

    private final String status;
    private final String transactionId;
    private final String message;
    private final String checkoutUrl;

    public PaymentResponse(String status, String transactionId, String message) {
        this(status, transactionId, message, null);
    }

    public PaymentResponse(String status, String transactionId, String message, String checkoutUrl) {
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.checkoutUrl = checkoutUrl;
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

    public String getCheckoutUrl() {
        return checkoutUrl;
    }
}
