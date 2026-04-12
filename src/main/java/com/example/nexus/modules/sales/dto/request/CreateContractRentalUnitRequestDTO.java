package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractRentalUnitRequestDTO(
        @NotNull @Positive
        Long rentalUnitId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @Positive
        Integer status
) {}
