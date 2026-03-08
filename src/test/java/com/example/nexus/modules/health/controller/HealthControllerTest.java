package com.example.nexus.modules.health.controller;

import com.example.nexus.modules.health.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    @Test
    void shouldReturnUpHealthStatus() {
        HealthController controller = new HealthController();

        HealthResponse response = controller.health();

        assertEquals("UP", response.status());
        assertNotNull(response.timestamp());
    }
}
