package com.esprit.furhope.api.dto.auth;

public class LoginResponse {
    private boolean success;
    private Long userId;
    private String displayName;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, Long userId, String displayName, String message) {
        this.success = success;
        this.userId = userId;
        this.displayName = displayName;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
