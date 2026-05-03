package com.example.nexus.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddInventoryCountLineRequestDTO(
        @NotNull @Positive Long productId,
        @Positive Long lotId,
        @NotNull @Positive Long storageSpaceId,
        Integer systemQty,
        Integer physicalQty,
        Integer difference
) {}
