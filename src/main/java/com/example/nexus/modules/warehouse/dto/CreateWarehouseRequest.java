package com.example.nexus.modules.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateWarehouseRequest(

        @NotBlank
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal capacity,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal totalCapacityM2,

        String location,

        @NotNull
        @Positive
        Long cityId,

        Boolean active
) {
}