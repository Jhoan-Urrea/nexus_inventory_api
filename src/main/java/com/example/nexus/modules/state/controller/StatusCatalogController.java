package com.example.nexus.modules.state.controller;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;
import com.example.nexus.modules.state.service.StatusCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Status Catalog", description = "Catalogo maestro de estados por entidad")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StatusCatalogController {

    private final StatusCatalogService statusCatalogService;

    @Operation(summary = "Crear nuevo estado en el catalogo")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusCatalogResponseDTO> create(@Valid @RequestBody CreateStatusCatalogRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusCatalogService.create(request));
    }

    @Operation(summary = "Listar estados por tipo de entidad")
    @GetMapping("/entity-type/{entityTypeId}")
    @PreAuthorize("isAuthenticated()")
    public List<StatusCatalogResponseDTO> getByEntityType(@PathVariable Long entityTypeId) {
        return statusCatalogService.findByEntityType(entityTypeId);
    }

    @Operation(summary = "Obtener estado del catalogo por id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatusCatalogResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(statusCatalogService.findById(id));
    }

    @Operation(summary = "Actualizar estado del catalogo")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusCatalogResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusCatalogRequestDTO request
    ) {
        return ResponseEntity.ok(statusCatalogService.update(id, request));
    }

    @Operation(summary = "Eliminar estado del catalogo")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        statusCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
