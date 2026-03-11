package com.example.nexus.modules.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateWarehouseRequest(
        @NotBlank
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal capacity,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal totalCapacityM2,

        String location,

        @Positive
        Long cityId,

        Boolean active
) {
}
