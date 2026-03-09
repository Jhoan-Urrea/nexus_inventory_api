package com.example.nexus.modules.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWarehouseRequest(
        @NotBlank
        String name,

        String description,

        Integer capacity,

        @NotNull
        Integer totalCapacityM2,

        String location,

        @NotNull
        Long cityId,

        Boolean active
) {}
