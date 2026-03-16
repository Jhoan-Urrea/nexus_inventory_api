package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceResponseDTO;
import com.example.nexus.modules.warehouse.service.StorageSpaceService;
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
@RequestMapping("/api/storage-spaces")
@Tag(name = "Storage Spaces", description = "Gestión de posiciones/espacios de almacenamiento")
@RequiredArgsConstructor
public class StorageSpaceController {

    private final StorageSpaceService storageSpaceService;

    @Operation(summary = "Crear espacio de almacenamiento")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageSpaceResponseDTO> create(@Valid @RequestBody CreateStorageSpaceRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageSpaceService.create(request));
    }

    @Operation(summary = "Obtener espacio por código lógico")
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public StorageSpaceResponseDTO getByCode(@PathVariable String code) {
        return storageSpaceService.findByCode(code);
    }

    @Operation(summary = "Listar espacios por sector")
    @GetMapping("/sector/{sectorId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<StorageSpaceResponseDTO> getBySector(@PathVariable Long sectorId) {
        return storageSpaceService.findBySectorId(sectorId);
    }

    @Operation(summary = "Actualizar espacio de almacenamiento")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StorageSpaceResponseDTO update(@PathVariable Long id, @Valid @RequestBody UpdateStorageSpaceRequestDTO request) {
        return storageSpaceService.update(id, request);
    }

    @Operation(summary = "Eliminar espacio de almacenamiento")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storageSpaceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
