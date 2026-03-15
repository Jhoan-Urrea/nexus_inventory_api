package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateWarehouseRequestDTO(
        String name,
        String location,
        @DecimalMin("0.0") BigDecimal totalCapacityM2,
        @Positive Long cityId,
        @Positive Long statusCatalogId,
        @Positive Long warehouseTypeId,
        Boolean active
) {}
