package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.model.AuthTokens;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenLifecycleService {

    AuthTokens issueTokens(UserDetails userDetails);

    AuthTokens refreshToken(String refreshToken, String ipAddress);

    void logout(String refreshToken, String ipAddress);

    boolean isAccessTokenRevoked(String accessToken);
}
