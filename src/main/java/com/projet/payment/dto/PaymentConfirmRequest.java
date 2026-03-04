package com.projet.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentConfirmRequest {

    @NotBlank
    private String transactionId;

    @NotBlank
    private String confirmationCode;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
