package com.example.nexus.modules.sales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalUnitPricingDTO(
        Long rentalUnitId,
        String entityTypeName,
        String referenceType,
        Long referenceId,
        String referenceCode,
        String referenceName,
        BigDecimal basePrice,
        String currency,
        Boolean priceActive,
        LocalDateTime priceUpdatedAt,
        Long priceUpdatedBy
) {}
