package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record RentalBulkAvailabilityRequestDTO(
        @NotEmpty
        List<Long> rentalUnitIds,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {}
