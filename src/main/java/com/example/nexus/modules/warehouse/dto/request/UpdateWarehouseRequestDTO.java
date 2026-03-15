package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateWarehouseRequest(
        String name,
        BigDecimal totalCapacityM2,
        String location,
        @Positive Long cityId,
        @Positive Long statusCatalogId,
        @Positive Long warehouseTypeId,
        Boolean active
) {}
