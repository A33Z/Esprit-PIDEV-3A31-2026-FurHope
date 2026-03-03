package com.projet.payment.dto;

import java.time.LocalDateTime;

public class OrderHistoryResponse {

    private Long orderId;
    private double totalAmount;
    private String transactionId;
    private LocalDateTime createdAt;

    public OrderHistoryResponse() {
    }

    public OrderHistoryResponse(Long orderId, double totalAmount, String transactionId, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
