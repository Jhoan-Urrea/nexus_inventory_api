package com.example.nexus.modules.inventory.dto.request;

import java.time.LocalDate;

public record CreateLotRequestDTO(
        String lotNumber,
        LocalDate expirationDate,
        LocalDate productionDate
) {}
