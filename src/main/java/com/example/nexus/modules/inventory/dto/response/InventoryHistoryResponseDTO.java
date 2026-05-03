package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryHistoryResponseDTO(
        Long id,
        Long movementId,
        Integer quantityBefore,
        Integer quantityAfter,
        LocalDateTime createdAt
) {}
