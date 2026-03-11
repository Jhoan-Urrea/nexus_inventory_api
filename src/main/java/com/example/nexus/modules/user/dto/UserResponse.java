package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record UserResponse(
        @Schema(example = "100")
        Long id,

        @Schema(example = "cliente1")
        String username,

        @Schema(example = "cliente@empresa.com")
        String email,

        @Schema(example = "ACTIVE")
        String status,

        @Schema(example = "[\"CLIENT\"]")
        Set<String> roles,

        @Schema(example = "10")
        Long clientId
) {}