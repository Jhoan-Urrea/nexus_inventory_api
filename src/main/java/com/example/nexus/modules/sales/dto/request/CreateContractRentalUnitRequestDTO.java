package com.example.nexus.modules.sales.dto.request;

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

        /**
         * Campo legacy del front. El backend ya no confía en este valor y calcula el precio
         * desde el catálogo (rental_units.base_price) para construir el contrato.
         */
        BigDecimal price,

        @NotNull @Positive
        Integer status
) {}
