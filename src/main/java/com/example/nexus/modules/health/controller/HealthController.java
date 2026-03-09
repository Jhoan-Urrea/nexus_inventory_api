package com.example.nexus.modules.health.controller;

import com.example.nexus.modules.health.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Estado de salud del servicio")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio activo")
    })
    public HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }
}
