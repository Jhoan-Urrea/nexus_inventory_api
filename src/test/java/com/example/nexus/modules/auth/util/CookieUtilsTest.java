package com.example.nexus.modules.auth.util;

import com.example.nexus.config.AuthCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieUtilsTest {

    @Test
    void shouldIncludeConfiguredDomainWhenPresent() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setHttpOnly(true);
        properties.setSecure(true);
        properties.setSameSite("None");
        properties.setPath("/");
        properties.setDomain("inventory.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieUtils.addCookie(response, "access_token", "token-value", 900, properties);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertTrue(setCookieHeader.contains("Domain=inventory.example.com"));
    }

    @Test
    void shouldOmitDomainWhenBlank() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setHttpOnly(true);
        properties.setSecure(true);
        properties.setSameSite("None");
        properties.setPath("/");
        properties.setDomain(" ");

        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieUtils.addCookie(response, "access_token", "token-value", 900, properties);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertFalse(setCookieHeader.contains("Domain="));
    }

    @Test
    void shouldOmitDomainWhenNull() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setHttpOnly(true);
        properties.setSecure(true);
        properties.setSameSite("None");
        properties.setPath("/");
        properties.setDomain(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieUtils.addCookie(response, "access_token", "token-value", 900, properties);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertFalse(setCookieHeader.contains("Domain="));
    }
}
