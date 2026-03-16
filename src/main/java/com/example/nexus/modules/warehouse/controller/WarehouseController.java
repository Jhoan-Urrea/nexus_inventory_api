package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseResponseDTO;
import com.example.nexus.modules.warehouse.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
@Tag(name = "Warehouses", description = "Gestión de bodegas/almacenes")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Crear bodega")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponseDTO> create(@Valid @RequestBody CreateWarehouseRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @Operation(summary = "Listar bodegas", description = "Roles permitidos: ADMIN, WAREHOUSE_EMPLOYEE, WAREHOUSE_SUPERVISOR")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<WarehouseResponseDTO> getAll() {
        return warehouseService.findAll();
    }

    @Operation(summary = "Obtener bodega por ID", description = "Roles permitidos: ADMIN, WAREHOUSE_EMPLOYEE, WAREHOUSE_SUPERVISOR")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public WarehouseResponseDTO getById(@PathVariable Long id) {
        return warehouseService.findById(id);
    }

    @Operation(summary = "Actualizar bodega")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponseDTO update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequestDTO request) {
        return warehouseService.update(id, request);
    }

    @Operation(summary = "Eliminar bodega")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
