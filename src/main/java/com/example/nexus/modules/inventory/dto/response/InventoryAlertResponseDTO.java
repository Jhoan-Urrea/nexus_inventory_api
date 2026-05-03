package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryAlertResponseDTO(
        Long id,
        Long productId,
        Long lotId,
        Long storageSpaceId,
        String alertType,
        Integer currentQuantity,
        Boolean resolved,
        LocalDateTime createdAt
) {}
