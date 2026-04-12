package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateReservationRequestDTO(
        @NotNull @Positive
        Long clientId,

        @NotNull @Positive
        Long rentalUnitId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {}
