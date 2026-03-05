package com.projet.payment;

public final class OrderApiEndpoints {

    private OrderApiEndpoints() {
    }

    public static String getCreateOrderUrl() {
        return SpringPaymentServer.getPaymentProcessUrl().replace("/api/payment/process", "/api/orders/create");
    }

    public static String getCustomerOrdersUrl(String encodedEmail) {
        return SpringPaymentServer.getPaymentProcessUrl()
                .replace("/api/payment/process", "/api/orders/customer/" + encodedEmail);
    }
}
