package com.ra.identityservice.service.impl;

import com.ra.identityservice.dto.request.LoginRequest;
import com.ra.identityservice.dto.request.LogoutRequest;
import com.ra.identityservice.dto.request.RegisterRequest;
import com.ra.identityservice.dto.response.AuthResponse;
import com.ra.identityservice.dto.response.UserResponse;
import com.ra.identityservice.entity.RefreshToken;
import com.ra.identityservice.entity.User;
import com.ra.identityservice.repository.RefreshTokenRepository;
import com.ra.identityservice.repository.UserRepository;
import com.ra.identityservice.service.AuthService;
import com.ra.identityservice.service.RefreshTokenService;
import com.ra.identityservice.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           RefreshTokenService refreshTokenService,
                           RefreshTokenRepository refreshTokenRepository,
                           StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .role(request.getRole() != null ? request.getRole() : "USER")
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    public String generateTestToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return jwtUtils.generateAccessToken(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String requestToken) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token không tồn tại hoặc đã được sử dụng"));
        refreshTokenService.verifyExpiration(oldToken);
        refreshTokenRepository.delete(oldToken);
        User user = oldToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String token = request.getToken();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Claims claims = jwtUtils.extractAllClaims(token);
        String jti = claims.getId();
        Date expiration = claims.getExpiration();

        long ttlInSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        if (ttlInSeconds > 0) {
            String redisKey = "blacklist:" + jti;
            redisTemplate.opsForValue().set(redisKey, "revoked", ttlInSeconds, TimeUnit.SECONDS);
        }

        String username = claims.getSubject();
        userRepository.findByUsername(username).ifPresent(refreshTokenRepository::deleteByUser);
    }
}