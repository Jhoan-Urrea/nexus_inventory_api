package com.example.nexus.modules.sales.dto.response;

import java.time.LocalDateTime;

public record AvailabilitySummaryResponseDTO(
        Long warehouseId,
        Long sectorId,
        Long storageSpaceId,
        int totalUnits,
        int available,
        int reserved,
        int contracted,
        LocalDateTime checkedAt
) {}
