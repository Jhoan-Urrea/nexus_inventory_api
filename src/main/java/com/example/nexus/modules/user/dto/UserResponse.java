package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        @Schema(description = "ID del usuario", example = "100")
        Long id,

        @Schema(description = "Nombre de usuario", example = "cliente1")
        String username,

        @Schema(description = "Correo electrónico", example = "cliente@empresa.com")
        String email,

        @Schema(description = "Estado del usuario", example = "ACTIVE")
        String status,

        @Schema(description = "Nombres de los roles asignados", example = "[\"CLIENT\"]")
        Set<String> roles,

        @Schema(description = "ID de la ciudad", example = "1")
        Long cityId,

        @Schema(description = "ID del cliente (opcional)", example = "10")
        Long clientId,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt
) {}