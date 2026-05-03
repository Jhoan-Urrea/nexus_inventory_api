package com.example.nexus.modules.inventory.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LotResponseDTO(
        Long id,
        Long productId,
        String lotNumber,
        LocalDate expirationDate,
        LocalDate productionDate,
        LocalDateTime createdAt
) {}
