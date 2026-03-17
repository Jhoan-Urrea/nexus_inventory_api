package com.example.nexus.modules.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CityResponse(
        @Schema(description = "ID de la ciudad", example = "1")
        Long id,

        @Schema(description = "Nombre de la ciudad", example = "Medellín")
        String name,

        @Schema(description = "Descripción")
        String description,

        @Schema(description = "Código postal", example = "050001")
        String postalCode,

        @Schema(description = "ID del departamento/región", example = "1")
        Long departmentRegionId,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt
) {
}
