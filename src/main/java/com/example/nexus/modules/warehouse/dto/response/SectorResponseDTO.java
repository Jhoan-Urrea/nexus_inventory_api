package com.example.nexus.modules.warehouse.dto.response;

public record SectorResponseDTO(
        Long id,
        String code,
        String description,
        Long warehouseId,
        String warehouseName,
        Long statusCatalogId,
        Boolean active
) {}
