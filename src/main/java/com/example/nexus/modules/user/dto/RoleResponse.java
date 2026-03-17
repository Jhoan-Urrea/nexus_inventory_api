package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record RoleResponse(
        @Schema(description = "ID del rol", example = "1")
        Long id,

        @Schema(description = "Nombre del rol", example = "ADMIN")
        String name,

        @Schema(description = "Descripción del rol")
        String description,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt
) {
}
