package com.ra.identityservice.service;

import com.ra.identityservice.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String userId);
    RefreshToken verifyExpiration(RefreshToken token);
}