package com.example.nexus.config;

import com.example.nexus.modules.user.service.ClientService;
import com.example.nexus.modules.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigCsrfTest.CsrfProbeTestConfig.class)
@TestPropertySource(properties = {
        "app.security.csrf-enabled=true",
        "app.security.csrf-cookie-name=APP-XSRF-TOKEN",
        "app.security.csrf-header-name=X-APP-CSRF-TOKEN",
        "app.security.csrf-cookie-http-only=false",
        "app.security.csrf-cookie-secure=true",
        "app.security.csrf-cookie-same-site=None",
        "app.security.csrf-cookie-path=/",
        "app.cors.allowed-origin-patterns=http://localhost:3000",
        "app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
        "app.cors.allowed-headers=Authorization,Content-Type",
        "app.cors.max-age=3600",
        "app.cors.allow-credentials=true",
        "app.auth.cookies.access-token-name=access_token",
        "app.auth.cookies.refresh-token-name=refresh_token",
        "app.auth.cookies.http-only=true",
        "app.auth.cookies.secure=false",
        "app.auth.cookies.same-site=Lax",
        "app.auth.cookies.path=/",
        "app.auth.cookies.access-token-max-age-seconds=900",
        "app.auth.cookies.refresh-token-max-age-seconds=604800"
})
class SecurityConfigCsrfTest {

    private static final String CSRF_COOKIE_NAME = "APP-XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-APP-CSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ClientService clientService;

    @Test
    void csrfBootstrapEndpointShouldBePublicAndExposeReadableCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CSRF_COOKIE_NAME))
                .andExpect(cookie().httpOnly(CSRF_COOKIE_NAME, false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.headerName").value(CSRF_HEADER_NAME))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.parameterName").value("_csrf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.token").isString())
                .andReturn();

        var csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie);
        assertEquals("/", csrfCookie.getPath());
        assertEquals("None", csrfCookie.getAttribute("SameSite"));
        assertEquals(true, csrfCookie.getSecure());
        assertEquals(
                csrfCookie.getValue(),
                objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText()
        );
        assertEquals(
                1L,
                java.util.Arrays.stream(result.getResponse().getCookies())
                        .filter(cookie -> CSRF_COOKIE_NAME.equals(cookie.getName()))
                        .count()
        );
    }

    @Test
    void protectedGetShouldNotRequireCsrf() throws Exception {
        mockMvc.perform(get("/api/csrf-probe")
                        .with(user("csrf-user").roles("USER")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void mutatingRequestsShouldRequireCsrf(String method) throws Exception {
        mockMvc.perform(authenticated(requestWithoutCsrf(method)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void mutatingRequestsShouldAcceptConfiguredCsrfHeader(String method) throws Exception {
        var csrfBootstrap = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = csrfBootstrap.getResponse().getCookie(CSRF_COOKIE_NAME).getValue();

        mockMvc.perform(authenticated(requestWithoutCsrf(method))
                        .header(CSRF_HEADER_NAME, csrfToken)
                        .cookie(csrfBootstrap.getResponse().getCookies())
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder requestWithoutCsrf(String method) {
        return switch (method) {
            case "POST" -> post("/api/csrf-probe").content("probe");
            case "PUT" -> put("/api/csrf-probe").content("probe");
            case "DELETE" -> delete("/api/csrf-probe");
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder builder) {
        return builder.with(user("csrf-user").roles("USER"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CsrfProbeTestConfig {

        @Bean
        CsrfProbeController csrfProbeController() {
            return new CsrfProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/csrf-probe")
    static class CsrfProbeController {

        @GetMapping
        String read() {
            return "ok";
        }

        @PostMapping
        String create() {
            return "ok";
        }

        @PutMapping
        String update() {
            return "ok";
        }

        @DeleteMapping
        String remove() {
            return "ok";
        }
    }
}
