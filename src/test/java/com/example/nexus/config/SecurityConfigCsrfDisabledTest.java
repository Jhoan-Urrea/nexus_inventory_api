package com.example.nexus.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigCsrfDisabledTest.CsrfDisabledProbeTestConfig.class)
@TestPropertySource(properties = {
        "app.security.csrf-enabled=false",
        "app.security.csrf-cookie-name=APP-XSRF-TOKEN",
        "app.security.csrf-header-name=X-APP-CSRF-TOKEN",
        "app.security.csrf-cookie-http-only=true",
        "app.cors.allowed-origin-patterns=http://localhost:3000",
        "app.cors.allowed-methods=GET,POST,OPTIONS",
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
class SecurityConfigCsrfDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mutatingRequestsShouldSucceedWithoutCsrfWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/csrf-disabled-probe")
                        .with(user("csrf-user").roles("USER"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("probe"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfCookieShouldNotBeIssuedWhenCsrfIsDisabled() throws Exception {
        mockMvc.perform(get("/api/csrf-disabled-probe")
                        .with(user("csrf-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("APP-XSRF-TOKEN"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CsrfDisabledProbeTestConfig {

        @Bean
        CsrfDisabledProbeController csrfDisabledProbeController() {
            return new CsrfDisabledProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/csrf-disabled-probe")
    static class CsrfDisabledProbeController {

        @GetMapping
        String read() {
            return "ok";
        }

        @PostMapping
        String create() {
            return "ok";
        }
    }
}
