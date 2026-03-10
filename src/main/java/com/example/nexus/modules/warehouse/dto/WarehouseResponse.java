package com.example.nexus.modules.warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String name,
        String description,
        BigDecimal capacity,
        BigDecimal availableCapacityM2,
        BigDecimal totalCapacityM2,
        String location,
        Boolean active,
        Long cityId,
        String cityName,
        LocalDateTime createdAt
) {
}
