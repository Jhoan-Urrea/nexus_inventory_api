package com.example.nexus.modules.sales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractResponseDTO(
        Long contractId,
        Long clientId,
        String clientName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount,
        Integer status,
        String statusName,
        List<ContractRentalUnitResponseDTO> contractRentalUnits
) {}
