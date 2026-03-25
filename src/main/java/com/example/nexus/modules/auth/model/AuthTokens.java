package com.example.nexus.modules.auth.model;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
