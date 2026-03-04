package com.projet.payment;

public final class PromoApiEndpoints {

    private PromoApiEndpoints() {
    }

    public static String getPromoApplyUrl() {
        return SpringPaymentServer.getPaymentProcessUrl().replace("/api/payment/process", "/api/promo/apply");
    }
}
