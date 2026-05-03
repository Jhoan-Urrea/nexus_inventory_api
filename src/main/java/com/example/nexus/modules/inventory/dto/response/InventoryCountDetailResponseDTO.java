package com.example.nexus.modules.inventory.dto.response;

public record InventoryCountDetailResponseDTO(
        Long id,
        Long countId,
        Long productId,
        Long lotId,
        Long storageSpaceId,
        Integer systemQty,
        Integer physicalQty,
        Integer difference
) {}
