package com.example.nexus.modules.auth.exception;

import java.time.Instant;

public record AuthErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
