package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceTypeResponseDTO;
import com.example.nexus.modules.warehouse.service.StorageSpaceTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage-space-types")
@Tag(name = "Storage Space Types", description = "Tipos de configuracion de espacios")
@RequiredArgsConstructor
public class StorageSpaceTypeController {

    private final StorageSpaceTypeService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<StorageSpaceTypeResponseDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StorageSpaceTypeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageSpaceTypeResponseDTO> create(@Valid @RequestBody CreateStorageSpaceTypeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageSpaceTypeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStorageSpaceTypeRequestDTO request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
