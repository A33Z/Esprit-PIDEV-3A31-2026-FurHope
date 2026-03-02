package com.esprit.furhope.api.web;

import com.esprit.furhope.api.dto.auth.LoginRequest;
import com.esprit.furhope.api.dto.auth.LoginResponse;
import com.esprit.furhope.api.dto.auth.SignupRequest;
import com.esprit.furhope.api.dto.auth.SignupResponse;
import com.esprit.furhope.api.service.AuthApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApiService authApiService;

    public AuthController(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authApiService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody SignupRequest request) {
        return authApiService.signup(request);
    }

    @GetMapping("/users/{userId}/displayName")
    public Map<String, String> userDisplayName(@PathVariable long userId) {
        Map<String, String> result = new HashMap<>();
        result.put("displayName", authApiService.getUserNameById(userId));
        return result;
    }
}
