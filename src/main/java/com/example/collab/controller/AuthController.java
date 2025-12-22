package com.example.collab.controller;

import com.example.collab.common.ApiResponse;
import com.example.collab.dto.AuthResponse;
import com.example.collab.dto.LoginRequest;
import com.example.collab.dto.RegisterRequest;
import com.example.collab.dto.ResetConfirmRequest;
import com.example.collab.dto.ResetRequest;
import com.example.collab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        return ApiResponse.ok(authService.login(request, ip));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/reset/request")
    public ApiResponse<Map<String, Object>> requestReset(@Valid @RequestBody ResetRequest request) {
        return ApiResponse.ok(authService.requestReset(request));
    }

    @PostMapping("/reset/confirm")
    public ApiResponse<Void> confirmReset(@Valid @RequestBody ResetConfirmRequest request) {
        authService.confirmReset(request);
        return ApiResponse.ok(null);
    }
}
