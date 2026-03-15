package com.example.nexus.modules.warehouse.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        BigDecimal totalCapacityM2,
        String location,
        Boolean active,
        Long cityId,
        String cityName,
        String statusName,     // Para mostrar "Activo", "Mantenimiento"
        String typeName,       // Para mostrar "Distribución", "Cross-docking"
        LocalDateTime createdAt
) {}
