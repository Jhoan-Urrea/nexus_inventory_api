package com.example.nexus.modules.health.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        Instant timestamp
) {
}
