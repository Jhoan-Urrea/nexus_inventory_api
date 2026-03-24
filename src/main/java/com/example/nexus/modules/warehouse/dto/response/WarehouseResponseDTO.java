package com.example.nexus.modules.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarehouseResponseDTO(
        Long id,
        String code,
        String name,
        BigDecimal totalCapacityM2,
        String location,
        Boolean active,

        @Schema(
                description = "Estado operativo de la bodega (usar para chips/badges). No confundir con statusName del catálogo.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE"}
        )
        String operationalStatus,

        @Schema(
                description = "Etiqueta en español para mostrar en UI según active",
                example = "Activo"
        )
        String operationalLabel,

        Long cityId,
        String cityName,
        @Schema(description = "Descripción del estado en catálogo (operacional del negocio, ej. mantenimiento)")
        String statusName,
        String typeName,
        LocalDateTime createdAt
) {}
