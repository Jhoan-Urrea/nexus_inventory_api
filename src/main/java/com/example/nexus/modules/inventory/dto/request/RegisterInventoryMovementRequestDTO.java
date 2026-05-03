package com.example.nexus.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterInventoryMovementRequestDTO(
        @NotNull @Positive Long productId,
        @Positive Long lotId,
        @NotNull @Positive Long storageSpaceId,
        @NotNull @Positive Long movementTypeId,
        Long movementSubtypeId,
        @NotNull @Positive Integer quantity,
        String note
) {}
