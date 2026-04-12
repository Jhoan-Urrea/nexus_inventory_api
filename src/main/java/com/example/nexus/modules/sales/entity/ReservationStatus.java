package com.example.nexus.modules.sales.entity;

public enum ReservationStatus {
    PENDING(1),
    CONFIRMED(2),
    EXPIRED(3),
    CANCELLED(4);

    private final int code;

    ReservationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
