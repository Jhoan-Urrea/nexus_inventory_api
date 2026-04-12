package com.example.nexus.modules.sales.entity;

import java.util.Locale;

public enum PaymentStatus {
    PENDING,
    APPROVED,
    FAILED,
    REFUNDED;

    public static PaymentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return PaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
