package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryProductResponseDTO(
        Long id,
        String name,
        String barcode,
        String productType,
        String unit,
        Boolean active,
        LocalDateTime createdAt
) {}
