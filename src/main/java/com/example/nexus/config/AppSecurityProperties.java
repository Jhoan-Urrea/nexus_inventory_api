package com.example.nexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Flags de endurecimiento HTTP (OWASP / producción).
 * Sobrescribir en {@code application-prod.properties} según el entorno.
 */
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * Confiar en X-Forwarded-For / X-Real-IP (solo detrás de proxy de confianza).
     */
    private boolean trustForwardedHeaders = false;

    /**
     * Si true, documentación OpenAPI / Swagger UI sin JWT (solo recomendado en dev).
     */
    private boolean permitSwaggerDocumentation = true;

    /**
     * Si true, {@code /api/locations/**} público (p. ej. registro con selector de ubicación).
     */
    private boolean permitPublicLocations = true;

    /**
     * Si true, {@code /actuator/health} y subrutas sin JWT (probes de orquestador / LB).
     */
    private boolean actuatorHealthPublic = true;

    /**
     * Si true, el resto de {@code /actuator/**} requiere rol ADMIN (p. ej. scrape Prometheus con Bearer).
     */
    private boolean actuatorAdminOnly = false;

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public boolean isPermitSwaggerDocumentation() {
        return permitSwaggerDocumentation;
    }

    public void setPermitSwaggerDocumentation(boolean permitSwaggerDocumentation) {
        this.permitSwaggerDocumentation = permitSwaggerDocumentation;
    }

    public boolean isPermitPublicLocations() {
        return permitPublicLocations;
    }

    public void setPermitPublicLocations(boolean permitPublicLocations) {
        this.permitPublicLocations = permitPublicLocations;
    }

    public boolean isActuatorHealthPublic() {
        return actuatorHealthPublic;
    }

    public void setActuatorHealthPublic(boolean actuatorHealthPublic) {
        this.actuatorHealthPublic = actuatorHealthPublic;
    }

    public boolean isActuatorAdminOnly() {
        return actuatorAdminOnly;
    }

    public void setActuatorAdminOnly(boolean actuatorAdminOnly) {
        this.actuatorAdminOnly = actuatorAdminOnly;
    }
}
