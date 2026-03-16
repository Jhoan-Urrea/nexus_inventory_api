package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;
import com.example.nexus.modules.warehouse.service.MaintenanceOrderService;
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
@RequestMapping("/api/maintenance-orders")
@Tag(name = "Maintenance Orders", description = "Gestión de órdenes de mantenimiento preventivo/correctivo")
@RequiredArgsConstructor
public class MaintenanceOrderController {

    private final MaintenanceOrderService maintenanceOrderService;

    @Operation(summary = "Crear orden de mantenimiento (XOR: Bodega, Sector o Espacio)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceOrderResponseDTO> create(@Valid @RequestBody CreateMaintenanceOrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceOrderService.create(request));
    }

    @Operation(summary = "Marcar una orden como completada")
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public MaintenanceOrderResponseDTO complete(@PathVariable Long id) {
        return maintenanceOrderService.completeOrder(id);
    }

    @Operation(summary = "Listar todas las órdenes de mantenimiento")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<MaintenanceOrderResponseDTO> getAll() {
        return maintenanceOrderService.findAll();
    }
}
