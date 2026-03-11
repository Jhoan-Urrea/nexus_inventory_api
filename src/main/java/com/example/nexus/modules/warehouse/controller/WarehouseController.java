package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.CreateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.UpdateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.WarehouseResponse;
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
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @Operation(summary = "Listar bodegas")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<WarehouseResponse> getAll() {
        return warehouseService.getAllWarehouses();
    }

    @Operation(summary = "Obtener bodega por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public WarehouseResponse getById(@PathVariable Long id) {
        return warehouseService.getWarehouseById(id);
    }

    @Operation(summary = "Actualizar bodega")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest request) {
        return warehouseService.updateWarehouse(id, request);
    }

    @Operation(summary = "Eliminar bodega")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
