package com.example.nexus.modules.inventory.service;

import com.example.nexus.modules.inventory.dto.request.*;
import com.example.nexus.modules.inventory.dto.response.*;

import java.util.List;

public interface InventoryProcessService {

    InventoryProductResponseDTO createProduct(CreateInventoryProductRequestDTO request);

    List<InventoryProductResponseDTO> findAllProducts();

    LotResponseDTO createLot(Long productId, CreateLotRequestDTO request);

    List<LotResponseDTO> findLotsByProduct(Long productId);

    List<MovementTypeResponseDTO> findMovementTypes();

    List<MovementSubtypeResponseDTO> findMovementSubtypes(Long typeId);

    List<InventoryBalanceResponseDTO> findBalances(Long storageSpaceId, Long productId);

    InventoryMovementResponseDTO registerMovement(RegisterInventoryMovementRequestDTO request);

    List<InventoryMovementResponseDTO> findRecentMovements();

    List<InventoryHistoryResponseDTO> findRecentHistory();

    List<InventoryAlertResponseDTO> findAlerts(boolean openOnly);

    void resolveAlert(Long alertId);

    InventoryCountResponseDTO startInventoryCount(CreateInventoryCountRequestDTO request);

    List<InventoryCountResponseDTO> findAllCounts();

    InventoryCountDetailResponseDTO addCountLine(Long countId, AddInventoryCountLineRequestDTO request);

    List<InventoryCountDetailResponseDTO> findCountLines(Long countId);

    InventoryCountResponseDTO completeCount(Long countId);
}
