package com.example.nexus.modules.warehouse.dto.request;

import java.time.LocalDateTime;

public record UpdateMaintenanceOrderRequestDTO(
        String status, // Para cambiar de PENDING a IN_PROGRESS o COMPLETED
        String description,
        LocalDateTime completedDate
) {}
