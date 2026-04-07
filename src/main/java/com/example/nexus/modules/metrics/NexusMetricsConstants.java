package com.example.nexus.modules.metrics;

/**
 * Nombres Micrometer (Prometheus: puntos → guiones bajos, sufijos _total / _seconds según tipo).
 * Solo contadores, timers y gauges de estado de negocio — las tasas y % se derivan en Prometheus.
 */
public final class NexusMetricsConstants {

    private NexusMetricsConstants() {
    }

    public static final String AUTH_LOGIN_SUCCESS_TOTAL = "auth.login.success";

    public static final String AUTH_LOGIN_FAILED_TOTAL = "auth.login.failed";

    public static final String AUTH_LOGIN_ATTEMPTS_TOTAL = "auth.login.attempts";

    public static final String AUTH_LOGOUT_TOTAL = "auth.logout";

    /** Latencia POST /api/auth/login (p95 en Timer). */
    public static final String AUTH_LOGIN_DURATION = "auth.login.duration";

    /** Solicitudes HTTP a /api/auth/** */
    public static final String AUTH_HTTP_REQUESTS_TOTAL = "auth.http.requests";

    /** Respuestas 4xx en /api/auth/** (incluye 403). */
    public static final String AUTH_HTTP_4XX_TOTAL = "auth.http.errors.4xx";

    /** Respuestas 5xx en /api/auth/** */
    public static final String AUTH_HTTP_5XX_TOTAL = "auth.http.errors.5xx";

    /** Respuestas 403 en /api/auth/** (subconjunto de 4xx; útil para alertas de seguridad). */
    public static final String AUTH_HTTP_SECURITY_REJECT_TOTAL = "auth.http.security.reject";

    /**
     * Ocupación por bodega (0–100). Gauge de estado desde BD; no es una tasa HTTP.
     * Tag: warehouse_id.
     */
    public static final String WAREHOUSE_OCCUPANCY_RATIO = "warehouse.occupancy.ratio";

    /** Espacios de almacenamiento por bodega. Tag: warehouse_id. */
    public static final String WAREHOUSE_STORAGE_SPACES = "warehouse.storage.spaces";

    /** Sectores por bodega. Tag: warehouse_id. */
    public static final String WAREHOUSE_SECTORS = "warehouse.sectors";

    /** Latencia p95 en operaciones mutadoras de estructura (bodegas/sectores/espacios). */
    public static final String WAREHOUSE_OPERATIONS_DURATION = "warehouse.operations.duration";

    /** Solicitudes HTTP en mutaciones de estructura (POST/PUT/PATCH/DELETE en rutas de bodega). */
    public static final String WAREHOUSE_HTTP_REQUESTS_TOTAL = "warehouse.http.requests";

    /** Respuestas 4xx en esas operaciones. */
    public static final String WAREHOUSE_HTTP_4XX_TOTAL = "warehouse.http.errors.4xx";

    /** Respuestas 5xx en esas operaciones. */
    public static final String WAREHOUSE_HTTP_5XX_TOTAL = "warehouse.http.errors.5xx";
}
