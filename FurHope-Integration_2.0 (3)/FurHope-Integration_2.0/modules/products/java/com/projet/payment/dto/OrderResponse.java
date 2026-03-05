package com.projet.payment.dto;

public class OrderResponse {

    private boolean success;
    private Long orderId;
    private String message;

    public OrderResponse() {
    }

    public OrderResponse(boolean success, Long orderId, String message) {
        this.success = success;
        this.orderId = orderId;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
