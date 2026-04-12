package com.example.nexus.modules.sales.dto.response;

import java.time.LocalDateTime;

public record RentalAvailabilityResponseDTO(
        RentalUnitResponseDTO rentalUnit,
        Boolean available,
        Boolean hasActiveReservation,
        Boolean hasContractConflict,
        String message,
        LocalDateTime checkedAt
) {}
