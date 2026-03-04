package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenLifecycleService {

    AuthResponse issueTokens(UserDetails userDetails);

    AuthResponse refreshToken(String refreshToken, String ipAddress);

    void logout(String accessToken, String refreshToken, String ipAddress);

    boolean isAccessTokenRevoked(String accessToken);
}
