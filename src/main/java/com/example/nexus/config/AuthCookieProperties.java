package com.example.nexus.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth.cookies")
public class AuthCookieProperties {

    @NotBlank
    private String accessTokenName;

    @NotBlank
    private String refreshTokenName;

    private boolean httpOnly;
    private boolean secure;

    @NotBlank
    @Pattern(
            regexp = "^(?i)(Strict|Lax|None)$",
            message = "app.auth.cookies.same-site must be one of Strict, Lax, None"
    )
    private String sameSite;

    @NotBlank
    @Pattern(
            regexp = "^/.*$",
            message = "app.auth.cookies.path must start with '/'"
    )
    private String path;

    private String domain;

    @Positive
    private long accessTokenMaxAgeSeconds;

    @Positive
    private long refreshTokenMaxAgeSeconds;

    public String getAccessTokenName() {
        return accessTokenName;
    }

    public void setAccessTokenName(String accessTokenName) {
        this.accessTokenName = accessTokenName;
    }

    public String getRefreshTokenName() {
        return refreshTokenName;
    }

    public void setRefreshTokenName(String refreshTokenName) {
        this.refreshTokenName = refreshTokenName;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getAccessTokenMaxAgeSeconds() {
        return accessTokenMaxAgeSeconds;
    }

    public void setAccessTokenMaxAgeSeconds(long accessTokenMaxAgeSeconds) {
        this.accessTokenMaxAgeSeconds = accessTokenMaxAgeSeconds;
    }

    public long getRefreshTokenMaxAgeSeconds() {
        return refreshTokenMaxAgeSeconds;
    }

    public void setRefreshTokenMaxAgeSeconds(long refreshTokenMaxAgeSeconds) {
        this.refreshTokenMaxAgeSeconds = refreshTokenMaxAgeSeconds;
    }

    @AssertTrue(message = "app.auth.cookies token names must not contain whitespace, commas, semicolons, or control characters")
    public boolean isCookieNamesValid() {
        return isValidCookieName(accessTokenName) && isValidCookieName(refreshTokenName);
    }

    @AssertTrue(message = "app.auth.cookies.domain must be blank or a valid cookie domain")
    public boolean isDomainValid() {
        if (domain == null || domain.isBlank()) {
            return true;
        }

        String normalizedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
        if (normalizedDomain.isBlank() || normalizedDomain.startsWith(".") || normalizedDomain.endsWith(".")) {
            return false;
        }

        return normalizedDomain.matches("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");
    }

    @AssertTrue(message = "app.auth.cookies.secure must be true when app.auth.cookies.same-site is None")
    public boolean isSameSiteCompatibleWithSecure() {
        if (sameSite == null) {
            return true;
        }

        return !"none".equalsIgnoreCase(sameSite.trim()) || secure;
    }

    private boolean isValidCookieName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (char currentChar : value.toCharArray()) {
            if (Character.isWhitespace(currentChar)
                    || currentChar == ','
                    || currentChar == ';'
                    || Character.isISOControl(currentChar)) {
                return false;
            }
        }

        return true;
    }
}
