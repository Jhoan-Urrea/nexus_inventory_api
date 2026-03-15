package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.request.CreateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.SectorResponseDTO;
import com.example.nexus.modules.warehouse.service.SectorService;
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
@RequestMapping("/api/sectors")
@Tag(name = "Sectors", description = "Gestión de sectores dentro de las bodegas")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    @Operation(summary = "Crear sector")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectorResponseDTO> create(@Valid @RequestBody CreateSectorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.create(request));
    }

    @Operation(summary = "Listar sectores por bodega")
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_EMPLOYEE','WAREHOUSE_SUPERVISOR')")
    public List<SectorResponseDTO> getByWarehouse(@PathVariable Long warehouseId) {
        return sectorService.findByWarehouseId(warehouseId);
    }

    @Operation(summary = "Actualizar sector")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SectorResponseDTO update(@PathVariable Long id, @Valid @RequestBody UpdateSectorRequestDTO request) {
        return sectorService.update(id, request);
    }

    @Operation(summary = "Eliminar sector")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
