package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryMovementResponseDTO(
        Long id,
        Long productId,
        Long lotId,
        Long storageSpaceId,
        Long userId,
        Long movementTypeId,
        String movementTypeName,
        Long movementSubtypeId,
        String movementSubtypeName,
        Integer quantity,
        String note,
        LocalDateTime createdAt
) {}
