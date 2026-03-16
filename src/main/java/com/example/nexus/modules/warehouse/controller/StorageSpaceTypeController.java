package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
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
@Tag(name = "Storage Space Types", description = "Tipos de configuración de espacios")
@RequiredArgsConstructor
public class StorageSpaceTypeController {

    private final StorageSpaceTypeService service; // Asumiendo interface creada

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<StorageSpaceTypeResponseDTO> getAll() {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageSpaceTypeResponseDTO> create(@Valid @RequestBody CreateStorageSpaceTypeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
