package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateContractRequest(
        @NotBlank
        @Size(max = 100)
        String reservationToken,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @NotEmpty
        List<@Valid CreateContractRentalUnitRequestDTO> contractRentalUnits
) {}
