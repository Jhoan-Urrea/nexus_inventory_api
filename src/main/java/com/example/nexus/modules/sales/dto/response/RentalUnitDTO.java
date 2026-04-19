package com.example.nexus.modules.sales.dto.response;

public record RentalUnitDTO(
        Long rentalUnitId,
        Long warehouseId,
        Long sectorId,
        Long storageSpaceId,
        Long entityTypeId,
        String availabilityStatus
) {}
