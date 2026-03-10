package com.example.nexus.modules.health.controller;

import com.example.nexus.modules.health.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Endpoints para verificar el estado de la API")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Obtiene el estado de la API")
    public HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }
}
