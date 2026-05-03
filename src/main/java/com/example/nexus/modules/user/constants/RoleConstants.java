package com.example.nexus.modules.user.constants;

public final class RoleConstants {
    private RoleConstants() {}

    public static final String ADMIN = "ADMIN";
    /** Acceso a bodegas, sectores, espacios y órdenes de mantenimiento (no al módulo nuevo de inventario). */
    public static final String WAREHOUSE_EMPLOYEE = "WAREHOUSE_EMPLOYEE";
    /** Solo proceso de inventario (`/api/inventory/**`). */
    public static final String WAREHOUSE_OPERATOR = "WAREHOUSE_OPERATOR";
    public static final String WAREHOUSE_SUPERVISOR = "WAREHOUSE_SUPERVISOR";
    public static final String USER = "USER";
    public static final String CLIENT = "CLIENT";
    public static final String SALES_AGENT = "SALES_AGENT";
}
