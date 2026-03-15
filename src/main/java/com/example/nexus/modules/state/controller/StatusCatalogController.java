package com.example.nexus.modules.state.controller;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;
import com.example.nexus.modules.state.service.StatusCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/status-catalogs")
@Tag(name = "Status Catalog", description = "Catálogo maestro de estados por entidad")
@RequiredArgsConstructor
public class StatusCatalogController {

    private final StatusCatalogService statusCatalogService;

    @Operation(summary = "Crear nuevo estado en el catálogo")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusCatalogResponseDTO> create(@Valid @RequestBody CreateStatusCatalogRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusCatalogService.create(request));
    }

    @Operation(summary = "Listar estados por tipo de entidad (Warehouse, Sector, etc.)")
    @GetMapping("/entity-type/{entityTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<StatusCatalogResponseDTO> getByEntityType(@PathVariable Long entityTypeId) {
        return statusCatalogService.findByEntityType(entityTypeId);
    }
}
