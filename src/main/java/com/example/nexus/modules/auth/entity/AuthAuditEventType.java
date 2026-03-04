package com.example.nexus.modules.auth.entity;

public enum AuthAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    TOKEN_REFRESH,
    LOGOUT,
    PASSWORD_FORGOT,
    PASSWORD_RESET
}
