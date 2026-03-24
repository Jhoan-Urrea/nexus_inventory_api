package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseResponseDTO;
import com.example.nexus.modules.warehouse.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Gestión de bodegas/almacenes")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear bodega")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bodega creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<WarehouseResponseDTO> create(@Valid @RequestBody CreateWarehouseRequestDTO request) {
        WarehouseResponseDTO response = warehouseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar bodegas")
    public ResponseEntity<List<WarehouseResponseDTO>> getAll() {
        return ResponseEntity.ok(warehouseService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener bodega por id")
    public ResponseEntity<WarehouseResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar bodega")
    public ResponseEntity<WarehouseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequestDTO request) {
        return ResponseEntity.ok(warehouseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Inhabilitar bodega (borrado lógico)",
            description = "No elimina el registro: pone active=false y devuelve el estado actualizado para que el cliente pueda refrescar UI sin un GET adicional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bodega inhabilitada; cuerpo con operationalStatus/operationalLabel actualizados"),
            @ApiResponse(responseCode = "404", description = "Bodega no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<WarehouseResponseDTO> delete(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.delete(id));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inhabilitar bodega (borrado lógico)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bodega inhabilitada"),
            @ApiResponse(responseCode = "404", description = "Bodega no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<WarehouseResponseDTO> disable(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.disable(id));
    }
}