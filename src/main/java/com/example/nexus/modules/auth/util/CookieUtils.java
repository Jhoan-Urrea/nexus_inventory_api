package com.example.nexus.modules.auth.util;

import com.example.nexus.config.AuthCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private CookieUtils() {
    }

    public static void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds,
            AuthCookieProperties properties
    ) {
        response.addHeader("Set-Cookie", buildCookie(name, value, maxAgeSeconds, properties));
    }

    public static void clearCookie(
            HttpServletResponse response,
            String name,
            AuthCookieProperties properties
    ) {
        response.addHeader("Set-Cookie", buildCookie(name, "", 0, properties));
    }

    public static String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String buildCookie(
            String name,
            String value,
            long maxAgeSeconds,
            AuthCookieProperties properties
    ) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(properties.isHttpOnly())
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getPath())
                .maxAge(maxAgeSeconds);

        if (properties.getDomain() != null && !properties.getDomain().isBlank()) {
            cookieBuilder.domain(properties.getDomain());
        }

        return cookieBuilder.build().toString();
    }
}
