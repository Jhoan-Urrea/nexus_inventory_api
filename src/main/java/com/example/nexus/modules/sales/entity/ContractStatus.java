package com.example.nexus.modules.sales.entity;

public enum ContractStatus {
    DRAFT(1),
    ACTIVE(2),
    COMPLETED(3),
    CANCELLED(4);

    private final int code;

    ContractStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ContractStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContractStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contract status code: " + code);
    }
}
