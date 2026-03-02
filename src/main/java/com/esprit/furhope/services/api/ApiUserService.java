package com.esprit.furhope.services.api;

import com.esprit.furhope.integration.api.ApiClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ApiUserService {

    private final ApiClient apiClient = new ApiClient();

    public LoginResult login(String email, String password) throws SQLException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);
            LoginResult result = apiClient.post("/api/auth/login", null, body, LoginResult.class);
            return result == null ? new LoginResult(false, null, null, "No response") : result;
        } catch (IOException e) {
            throw asSqlException("Login API failed", e);
        }
    }

    public SignupResult signup(String name, String email, String phone, String password) throws SQLException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("email", email);
            body.put("phone", phone);
            body.put("password", password);
            SignupResult result = apiClient.post("/api/auth/signup", null, body, SignupResult.class);
            return result == null ? new SignupResult(false, "No response") : result;
        } catch (IOException e) {
            throw asSqlException("Signup API failed", e);
        }
    }

    private SQLException asSqlException(String message, Exception cause) {
        return new SQLException(message + ": " + cause.getMessage(), cause);
    }

    public static class LoginResult {
        private boolean success;
        private Long userId;
        private String displayName;
        private String message;

        public LoginResult() {
        }

        public LoginResult(boolean success, Long userId, String displayName, String message) {
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

    public static class SignupResult {
        private boolean success;
        private String message;

        public SignupResult() {
        }

        public SignupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
