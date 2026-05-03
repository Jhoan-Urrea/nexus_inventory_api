package com.example.nexus.modules.inventory.controller;

import com.example.nexus.modules.inventory.dto.request.*;
import com.example.nexus.modules.inventory.dto.response.*;
import com.example.nexus.modules.inventory.service.InventoryProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WAREHOUSE_OPERATOR')")
@Tag(name = "Inventory", description = "Producto, lote, movimientos, conteos y alertas (rol WAREHOUSE_OPERATOR)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryProcessController {

    private final InventoryProcessService inventoryProcessService;

    @PostMapping("/products")
    @Operation(summary = "Registrar producto de inventario")
    public ResponseEntity<InventoryProductResponseDTO> createProduct(
            @Valid @RequestBody CreateInventoryProductRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryProcessService.createProduct(request));
    }

    @GetMapping("/products")
    @Operation(summary = "Listar productos de inventario")
    public ResponseEntity<List<InventoryProductResponseDTO>> listProducts() {
        return ResponseEntity.ok(inventoryProcessService.findAllProducts());
    }

    @PostMapping("/products/{productId}/lots")
    @Operation(
            summary = "Crear lote para un producto",
            description = "Solo permitido si el tipo de producto requiere lote; vencimiento obligatorio u opcional según product_type_config."
    )
    public ResponseEntity<LotResponseDTO> createLot(
            @PathVariable Long productId,
            @RequestBody(required = false) CreateLotRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryProcessService.createLot(productId, request));
    }

    @GetMapping("/products/{productId}/lots")
    @Operation(summary = "Listar lotes del producto")
    public ResponseEntity<List<LotResponseDTO>> listLots(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryProcessService.findLotsByProduct(productId));
    }

    @GetMapping("/movement-types")
    @Operation(summary = "Tipos de movimiento")
    public ResponseEntity<List<MovementTypeResponseDTO>> movementTypes() {
        return ResponseEntity.ok(inventoryProcessService.findMovementTypes());
    }

    @GetMapping("/movement-types/{typeId}/subtypes")
    @Operation(summary = "Subtipos por tipo de movimiento")
    public ResponseEntity<List<MovementSubtypeResponseDTO>> movementSubtypes(@PathVariable Long typeId) {
        return ResponseEntity.ok(inventoryProcessService.findMovementSubtypes(typeId));
    }

    @GetMapping("/balances")
    @Operation(summary = "Saldos por ubicación y/o producto (filtros opcionales)")
    public ResponseEntity<List<InventoryBalanceResponseDTO>> balances(
            @RequestParam(required = false) Long storageSpaceId,
            @RequestParam(required = false) Long productId
    ) {
        return ResponseEntity.ok(inventoryProcessService.findBalances(storageSpaceId, productId));
    }

    @PostMapping("/movements")
    @Operation(
            summary = "Registrar movimiento (dispara lógica en BD: stock, historial, alertas)",
            description = "lotId obligatorio solo si el tipo de producto requiere lote; si no requiere lote, omitir lotId (se usa lote técnico __SIN_LOTE__)."
    )
    public ResponseEntity<InventoryMovementResponseDTO> registerMovement(
            @Valid @RequestBody RegisterInventoryMovementRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryProcessService.registerMovement(request));
    }

    @GetMapping("/movements/recent")
    @Operation(summary = "Últimos movimientos")
    public ResponseEntity<List<InventoryMovementResponseDTO>> recentMovements() {
        return ResponseEntity.ok(inventoryProcessService.findRecentMovements());
    }

    @GetMapping("/history/recent")
    @Operation(summary = "Último historial de cantidades tras movimientos")
    public ResponseEntity<List<InventoryHistoryResponseDTO>> recentHistory() {
        return ResponseEntity.ok(inventoryProcessService.findRecentHistory());
    }

    @GetMapping("/alerts")
    @Operation(summary = "Alertas de inventario; openOnly=true solo sin resolver")
    public ResponseEntity<List<InventoryAlertResponseDTO>> alerts(
            @RequestParam(defaultValue = "true") boolean openOnly
    ) {
        return ResponseEntity.ok(inventoryProcessService.findAlerts(openOnly));
    }

    @PatchMapping("/alerts/{id}/resolve")
    @Operation(summary = "Marcar alerta como resuelta")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id) {
        inventoryProcessService.resolveAlert(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/counts")
    @Operation(summary = "Iniciar conteo cíclico (RF)")
    public ResponseEntity<InventoryCountResponseDTO> startCount(
            @RequestBody(required = false) CreateInventoryCountRequestDTO request
    ) {
        CreateInventoryCountRequestDTO body = request != null ? request : new CreateInventoryCountRequestDTO(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryProcessService.startInventoryCount(body));
    }

    @GetMapping("/counts")
    @Operation(summary = "Listar conteos")
    public ResponseEntity<List<InventoryCountResponseDTO>> listCounts() {
        return ResponseEntity.ok(inventoryProcessService.findAllCounts());
    }

    @PostMapping("/counts/{countId}/lines")
    @Operation(summary = "Agregar línea a un conteo abierto")
    public ResponseEntity<InventoryCountDetailResponseDTO> addCountLine(
            @PathVariable Long countId,
            @Valid @RequestBody AddInventoryCountLineRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryProcessService.addCountLine(countId, request));
    }

    @GetMapping("/counts/{countId}/lines")
    @Operation(summary = "Listar líneas de un conteo")
    public ResponseEntity<List<InventoryCountDetailResponseDTO>> listCountLines(@PathVariable Long countId) {
        return ResponseEntity.ok(inventoryProcessService.findCountLines(countId));
    }

    @PatchMapping("/counts/{countId}/complete")
    @Operation(summary = "Cerrar conteo")
    public ResponseEntity<InventoryCountResponseDTO> completeCount(@PathVariable Long countId) {
        return ResponseEntity.ok(inventoryProcessService.completeCount(countId));
    }
}
