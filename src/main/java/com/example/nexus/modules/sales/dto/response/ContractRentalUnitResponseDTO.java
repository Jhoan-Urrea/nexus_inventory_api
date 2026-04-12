package com.example.nexus.modules.sales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRentalUnitResponseDTO(
        Long contractRentalUnitId,
        Long contractId,
        RentalUnitResponseDTO rentalUnit,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal price,
        Integer status
) {}
