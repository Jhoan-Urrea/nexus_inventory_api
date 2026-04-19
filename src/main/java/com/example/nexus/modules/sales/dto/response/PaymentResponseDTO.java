package com.example.nexus.modules.sales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long paymentId,
        Long contractId,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String paymentStatus,
        String paymentMethod,
        String paymentReference,
        String paymentExternalReference,
        String stripeClientSecret
) {}
