package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMaintenanceOrderRequestDTO(
        @NotBlank String maintenanceType,
        @NotBlank String priority,
        @NotBlank String description,
        @NotNull LocalDateTime scheduledDate,
        Long warehouseId,      // Opcional por XOR
        Long sectorId,         // Opcional por XOR
        Long storageSpaceId    // Opcional por XOR
) {}
