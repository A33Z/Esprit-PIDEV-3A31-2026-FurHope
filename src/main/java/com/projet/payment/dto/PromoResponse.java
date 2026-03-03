package com.projet.payment.dto;

public class PromoResponse {

    private boolean valid;
    private double discount;
    private double finalAmount;
    private String message;

    public PromoResponse() {
    }

    public PromoResponse(boolean valid, double discount, double finalAmount, String message) {
        this.valid = valid;
        this.discount = discount;
        this.finalAmount = finalAmount;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
