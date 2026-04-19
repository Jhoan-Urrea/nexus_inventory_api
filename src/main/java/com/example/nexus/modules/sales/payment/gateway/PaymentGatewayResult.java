package com.example.nexus.modules.sales.payment.gateway;

import com.example.nexus.modules.sales.entity.PaymentStatus;

public record PaymentGatewayResult(
        PaymentStatus status,
        String paymentReference,
        String paymentExternalReference,
        String clientSecret
) {}
