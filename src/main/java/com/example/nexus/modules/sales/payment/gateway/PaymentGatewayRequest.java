package com.example.nexus.modules.sales.payment.gateway;

import com.example.nexus.modules.sales.entity.PaymentStatus;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        Long contractId,
        BigDecimal amount,
        String paymentMethod,
        String paymentReference,
        String paymentExternalReference,
        PaymentStatus requestedStatus,
        String chargeCurrency
) {}
