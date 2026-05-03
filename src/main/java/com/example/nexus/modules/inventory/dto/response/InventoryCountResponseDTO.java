package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDateTime;

public record InventoryCountResponseDTO(
        Long id,
        Long sectorId,
        Long userId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String status
) {}
