package com.example.nexus.modules.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken
) {
    public AuthResponse(String token) {
        this(token, null);
    }
}
