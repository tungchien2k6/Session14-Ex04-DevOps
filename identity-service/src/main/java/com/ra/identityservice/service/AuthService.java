package com.ra.identityservice.service;

import com.ra.identityservice.dto.request.LoginRequest;
import com.ra.identityservice.dto.request.LogoutRequest;
import com.ra.identityservice.dto.request.RegisterRequest;
import com.ra.identityservice.dto.response.AuthResponse;
import com.ra.identityservice.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    String generateTestToken(String username);
    AuthResponse refreshToken(String requestToken);
    void logout(LogoutRequest request);
}