package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.service.WarehouseTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/warehouse-types")
@RequiredArgsConstructor
@Tag(name = "Warehouse Types", description = "Tipos de bodega (warehouse_types)")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseTypeController {

    private final WarehouseTypeService warehouseTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear tipo de bodega")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo de bodega creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<WarehouseTypeResponseDTO> create(@Valid @RequestBody CreateWarehouseTypeRequestDTO request) {
        WarehouseTypeResponseDTO response = warehouseTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar tipos de bodega")
    public ResponseEntity<List<WarehouseTypeResponseDTO>> getAll() {
        return ResponseEntity.ok(warehouseTypeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener tipo de bodega por id")
    public ResponseEntity<WarehouseTypeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar tipo de bodega")
    public ResponseEntity<WarehouseTypeResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseTypeRequestDTO request) {
        return ResponseEntity.ok(warehouseTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar tipo de bodega")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
