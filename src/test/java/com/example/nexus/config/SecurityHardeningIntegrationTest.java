package com.example.nexus.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityHardeningIntegrationTest.HardeningProbeTestConfig.class)
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "app.security.permit-swagger-documentation=false",
        "app.security.actuator-admin-only=true",
        "app.security.actuator-health-public=true",
        "management.endpoints.web.exposure.include=health,prometheus"
})
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authNamespaceShouldNotExposeUnlistedEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/internal-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerShouldRequireAuthenticationWhenNotExplicitlyPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorShouldExposeOnlyHealthPublicly() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HardeningProbeTestConfig {

        @Bean
        AuthInternalProbeController authInternalProbeController() {
            return new AuthInternalProbeController();
        }
    }

    @RestController
    static class AuthInternalProbeController {

        @GetMapping("/api/auth/internal-probe")
        String probe() {
            return "ok";
        }
    }
}
