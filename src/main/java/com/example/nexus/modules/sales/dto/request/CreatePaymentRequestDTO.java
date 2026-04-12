package com.example.nexus.modules.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequestDTO(
        @NotNull @Positive
        Long contractId,

        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Size(max = 50)
        String paymentStatus,

        @Size(max = 50)
        String paymentMethod,

        @Size(max = 150)
        String paymentReference,

        @Size(max = 150)
        String paymentExternalReference
) {}
