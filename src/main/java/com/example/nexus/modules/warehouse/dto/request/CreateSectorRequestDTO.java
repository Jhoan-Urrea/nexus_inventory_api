package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSectorRequestDTO(
        @NotBlank String code,
        String description,
        @NotNull Long warehouseId,
        @NotNull Long statusCatalogId
) {}
