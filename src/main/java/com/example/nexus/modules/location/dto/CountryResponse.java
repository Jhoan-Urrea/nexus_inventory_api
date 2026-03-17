package com.example.nexus.modules.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CountryResponse(
        @Schema(description = "ID del país", example = "1")
        Long id,

        @Schema(description = "Nombre del país", example = "Colombia")
        String name,

        @Schema(description = "Descripción del país")
        String description,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt
) {
}
