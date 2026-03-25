package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateWarehouseRequestDTO(
        @Size(max = 100)
        String name,
        @Size(max = 255)
        String location,
        @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal totalCapacityM2,
        @Positive Long cityId,
        @Positive Long statusCatalogId,
        @Positive Long warehouseTypeId,
        Boolean active
) {}
