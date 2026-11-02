package com.ra.identityservice.controller;

import com.ra.identityservice.dto.request.LogoutRequest;
import com.ra.identityservice.dto.request.RefreshTokenRequest;
import com.ra.identityservice.dto.response.AuthResponse;
import com.ra.identityservice.dto.request.LoginRequest;
import com.ra.identityservice.dto.request.RegisterRequest;
import com.ra.identityservice.dto.response.UserResponse;
import com.ra.identityservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

    @GetMapping("/test-token")
    public ResponseEntity<String> testToken(@RequestParam String username) {
        String token = authService.generateTestToken(username);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}