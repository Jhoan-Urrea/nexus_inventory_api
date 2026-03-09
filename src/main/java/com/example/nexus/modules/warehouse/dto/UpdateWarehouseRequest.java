package com.example.nexus.modules.warehouse.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWarehouseRequest(
        @NotBlank
        String name,

        String description,

        Integer capacity,

        Integer totalCapacityM2,

        String location,

        Long cityId,

        Boolean active
) {}
