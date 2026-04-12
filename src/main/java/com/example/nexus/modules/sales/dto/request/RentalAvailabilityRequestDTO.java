package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record RentalAvailabilityRequestDTO(
        @NotNull @Positive
        Long rentalUnitId,

        LocalDate startDate,

        LocalDate endDate,

        Long excludeReservationId,

        Long excludeContractRentalUnitId
) {}
