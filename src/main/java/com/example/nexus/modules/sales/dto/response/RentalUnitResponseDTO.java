package com.example.nexus.modules.sales.dto.response;

public record RentalUnitResponseDTO(
        Long rentalUnitId,
        Long entityTypeId,
        String entityTypeName,
        String referenceType,
        Long referenceId,
        String referenceCode,
        String referenceName,
        String displayName
) {}
