package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateWarehouseRequestDTO(
        @NotBlank @Size(max = 20)
        String code, // Añadido: Identificador de negocio

        @NotBlank @Size(max = 100)
        String name,

        @NotNull @DecimalMin(value = "0.0") @Digits(integer = 10, fraction = 2)
        BigDecimal totalCapacityM2,

        @NotBlank @Size(max = 255)
        String location,

        @NotNull @Positive
        Long cityId,

        @NotNull @Positive
        Long statusCatalogId, // Añadido: Estado inicial (ej: ACTIVE)

        @NotNull @Positive
        Long warehouseTypeId // Añadido: Tipo (ej: Refrigerada, Seca)
) {}
