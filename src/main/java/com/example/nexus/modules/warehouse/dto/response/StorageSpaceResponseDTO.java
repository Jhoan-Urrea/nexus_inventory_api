package com.example.nexus.modules.warehouse.dto.response;

import java.math.BigDecimal;

public record StorageSpaceResponseDTO(
        Long id,
        String code, // Generado: "SEC-A-1-B-01"
        BigDecimal capacityM2,
        String aisle,
        String row,
        String level,
        String position,
        Boolean temperatureControl,
        Boolean humidityControl,
        Long sectorId,
        String typeName,
        Long statusCatalogId,
        Boolean active
) {}
