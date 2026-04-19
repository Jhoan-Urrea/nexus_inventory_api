package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record CreateReservationRequestDTO(
        @NotNull @Positive
        Long clientId,

        @NotEmpty
        List<Long> rentalUnitIds,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {}
