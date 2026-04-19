package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateRentalUnitPricingRequestDTO(
        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal basePrice,

        @NotNull
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @NotNull
        Boolean priceActive
) {}
