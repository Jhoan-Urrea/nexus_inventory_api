package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryBalanceResponseDTO(
        Long id,
        Long productId,
        String productName,
        Long lotId,
        String lotNumber,
        Long storageSpaceId,
        String storageSpaceCode,
        Integer quantity,
        LocalDateTime updatedAt
) {}
