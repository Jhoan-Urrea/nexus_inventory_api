package com.example.nexus.modules.state.controller;

import com.example.nexus.modules.state.dto.request.CreateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.response.EntityTypeResponseDTO;
import com.example.nexus.modules.state.service.EntityTypeService;
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
@RequestMapping("/api/entity-types")
@Tag(name = "Entity Type", description = "Tipos de entidad para catalogo maestro de estados")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EntityTypeController {

    private final EntityTypeService entityTypeService;

    @Operation(summary = "Crear nuevo tipo de entidad")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityTypeResponseDTO> create(@Valid @RequestBody CreateEntityTypeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entityTypeService.create(request));
    }

    @Operation(summary = "Listar tipos de entidad")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EntityTypeResponseDTO> getAll() {
        return entityTypeService.findAll();
    }

    @Operation(summary = "Obtener tipo de entidad por id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityTypeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(entityTypeService.findById(id));
    }

    @Operation(summary = "Actualizar tipo de entidad")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityTypeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEntityTypeRequestDTO request
    ) {
        return ResponseEntity.ok(entityTypeService.update(id, request));
    }

    @Operation(summary = "Eliminar tipo de entidad")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entityTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
