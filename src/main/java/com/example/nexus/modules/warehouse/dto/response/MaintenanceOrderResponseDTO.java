package com.example.nexus.modules.warehouse.dto.response;

import java.time.LocalDateTime;

public record MaintenanceOrderResponseDTO(
        Long id,
        String maintenanceType,
        String priority,
        String status,
        String description,
        LocalDateTime scheduledDate,
        LocalDateTime completedDate,
        Long warehouseId,
        Long sectorId,
        Long storageSpaceId,
        String targetName // Nombre del recurso (Bodega A, Sector B o Espacio X)
) {}
