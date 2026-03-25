package com.example.nexus.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Flags de endurecimiento HTTP (OWASP / producciÃ³n).
 * Sobrescribir en {@code application-prod.properties} segÃºn el entorno.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * Confiar en X-Forwarded-For / X-Real-IP (solo detrÃ¡s de proxy de confianza).
     */
    private boolean trustForwardedHeaders;

    /**
     * Si true, documentaciÃ³n OpenAPI / Swagger UI sin JWT (solo recomendado en dev).
     */
    private boolean permitSwaggerDocumentation;

    /**
     * Si true, {@code /api/locations/**} pÃºblico (p. ej. registro con selector de ubicaciÃ³n).
     */
    private boolean permitPublicLocations;

    /**
     * Si true, {@code /actuator/health} y subrutas sin JWT (probes de orquestador / LB).
     */
    private boolean actuatorHealthPublic;

    /**
     * Si true, el resto de {@code /actuator/**} requiere rol ADMIN (p. ej. scrape Prometheus con Bearer).
     */
    private boolean actuatorAdminOnly;

    /**
     * Si true, activa protecciÃ³n CSRF usando cookie repositorio para una migraciÃ³n progresiva a cookies.
     */
    private boolean csrfEnabled;

    /**
     * Nombre de la cookie CSRF expuesta al cliente para ecosistemas SPA.
     */
    @NotBlank
    private String csrfCookieName;

    /**
     * Nombre del header esperado para el token CSRF cuando la protecciÃ³n estÃ© activa.
     */
    @NotBlank
    private String csrfHeaderName;

    /**
     * Si true, marca la cookie CSRF como HttpOnly.
     */
    private boolean csrfCookieHttpOnly;

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

    public boolean isCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    public String getCsrfCookieName() {
        return csrfCookieName;
    }

    public void setCsrfCookieName(String csrfCookieName) {
        this.csrfCookieName = csrfCookieName;
    }

    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    public void setCsrfHeaderName(String csrfHeaderName) {
        this.csrfHeaderName = csrfHeaderName;
    }

    public boolean isCsrfCookieHttpOnly() {
        return csrfCookieHttpOnly;
    }

    public void setCsrfCookieHttpOnly(boolean csrfCookieHttpOnly) {
        this.csrfCookieHttpOnly = csrfCookieHttpOnly;
    }
}
