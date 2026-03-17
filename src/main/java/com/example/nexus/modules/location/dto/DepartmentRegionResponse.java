package com.example.nexus.modules.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DepartmentRegionResponse(
        @Schema(description = "ID del departamento/región", example = "1")
        Long id,

        @Schema(description = "Nombre del departamento o región", example = "Antioquia")
        String name,

        @Schema(description = "Descripción")
        String description,

        @Schema(description = "ID del país", example = "1")
        Long countryId,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt
) {
}
