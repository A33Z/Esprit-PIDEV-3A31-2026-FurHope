package com.esprit.entities;

import java.time.LocalDateTime;

public class Reponse {

    private int id;
    private int reclamationId;
    private int adminId;
    private int senderId;
    private String senderType;
    private String message;
    private Integer rating;
    private LocalDateTime createdAt;

    public Reponse() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAiMessage() {
        return "AI".equalsIgnoreCase(senderType);
    }

    public boolean isClientMessage() {
        return "CLIENT".equalsIgnoreCase(senderType);
    }

    public boolean isAdminMessage() {
        return "ADMIN".equalsIgnoreCase(senderType);
    }
}
