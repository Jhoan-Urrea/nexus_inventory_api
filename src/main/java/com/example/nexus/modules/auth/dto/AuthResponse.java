package com.example.nexus.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
    public AuthResponse(String accessToken) {
        this(accessToken, null);
    }

    /**
     * Backward-compat alias while clients migrate to accessToken.
     */
    @JsonProperty("token")
    public String token() {
        return accessToken;
    }
}
