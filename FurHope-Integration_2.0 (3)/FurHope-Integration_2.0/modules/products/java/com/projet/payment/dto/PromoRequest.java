package com.projet.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class PromoRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @Positive(message = "Amount must be greater than 0")
    private double amount;

    public PromoRequest() {
    }

    public PromoRequest(String code, double amount) {
        this.code = code;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
