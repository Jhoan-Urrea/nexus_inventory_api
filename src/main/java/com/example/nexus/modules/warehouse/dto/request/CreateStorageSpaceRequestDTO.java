package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateStorageSpaceRequestDTO(
        @NotBlank String aisle,
        @NotBlank String row,
        @NotBlank String level,
        @NotBlank String position,
        @NotNull @DecimalMin("0.0") BigDecimal capacityM2,
        @NotNull Boolean temperatureControl,
        @NotNull Boolean humidityControl,
        @NotNull Long sectorId,
        @NotNull Long storageSpaceTypeId,
        @NotNull Long statusCatalogId
) {}
