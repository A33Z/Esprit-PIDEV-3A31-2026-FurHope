package com.esprit.furhope.api.service;

import com.esprit.furhope.api.dto.auth.LoginResponse;
import com.esprit.furhope.api.dto.auth.SignupRequest;
import com.esprit.furhope.api.dto.auth.SignupResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthApiService {

    private final JdbcTemplate jdbcTemplate;

    public AuthApiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoginResponse login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return new LoginResponse(false, null, null, "Email and password are required");
        }

        return jdbcTemplate.query(
                "SELECT id_user, name, password_hash, password FROM api_user WHERE email=? LIMIT 1",
                rs -> {
                    if (!rs.next()) {
                        return new LoginResponse(false, null, null, "Invalid email or password");
                    }
                    String storedHash = rs.getString("password_hash");
                    String storedPassword = rs.getString("password");

                    boolean passwordOk;
                    if (storedHash != null && !storedHash.isBlank()) {
                        String inputHash = sha256Hex(password);
                        passwordOk = storedHash.equalsIgnoreCase(inputHash);
                    } else {
                        passwordOk = storedPassword != null && storedPassword.equals(password);
                    }

                    if (!passwordOk) {
                        return new LoginResponse(false, null, null, "Invalid email or password");
                    }
                    return new LoginResponse(true, rs.getLong("id_user"), rs.getString("name"), "ok");
                },
                email.trim()
        );
    }

    public SignupResponse signup(SignupRequest request) {
        if (request == null) {
            return new SignupResponse(false, "Request body is required");
        }

        String name = safeTrim(request.getName());
        String email = safeTrim(request.getEmail());
        String phone = safeTrim(request.getPhone());
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            return new SignupResponse(false, "All fields are required");
        }

        Integer exists = jdbcTemplate.query(
                "SELECT 1 FROM `user` WHERE email=? LIMIT 1",
                rs -> rs.next() ? 1 : null,
                email
        );
        if (exists != null) {
            return new SignupResponse(false, "Account already exists for this email");
        }

        int rows = jdbcTemplate.update(
                "INSERT INTO `user`(first_name, last_name, email, password, phone, role, active) VALUES (?,?,?,?,?,'VETERINAIRE',1)",
                firstName(name), lastName(name), email, password, phone
        );
        return rows > 0
                ? new SignupResponse(true, "Account created")
                : new SignupResponse(false, "Could not create account");
    }

    public String getUserNameById(long userId) {
        return jdbcTemplate.query(
                "SELECT name FROM api_user WHERE id_user = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("name") : null,
                userId
        );
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstName(String fullName) {
        String normalized = safeTrim(fullName);
        int i = normalized.indexOf(' ');
        return i > 0 ? normalized.substring(0, i).trim() : normalized;
    }

    private String lastName(String fullName) {
        String normalized = safeTrim(fullName);
        int i = normalized.indexOf(' ');
        if (i > 0 && i < normalized.length() - 1) {
            return normalized.substring(i + 1).trim();
        }
        return "_";
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
