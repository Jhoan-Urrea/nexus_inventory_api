package com.example.nexus.modules.sales.event;

import com.example.nexus.modules.sales.service.RentalUnitSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la sincronización fuera del hilo HTTP / callback post-commit para que el INSERT
 * use una conexión y transacción propias (evita competencia con Hikari en el mismo hilo).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalUnitSyncAsyncExecutor {

    private final RentalUnitSyncService rentalUnitSyncService;

    @Async
    public void syncAfterWarehouseCreated(Long warehouseId) {
        try {
            rentalUnitSyncService.createForWarehouse(warehouseId);
        } catch (Exception e) {
            log.error("Failed to automatically synchronize RentalUnit for Warehouse ID: {}", warehouseId, e);
        }
    }

    @Async
    public void syncAfterSectorCreated(Long sectorId) {
        try {
            rentalUnitSyncService.createForSector(sectorId);
        } catch (Exception e) {
            log.error("Failed to automatically synchronize RentalUnit for Sector ID: {}", sectorId, e);
        }
    }

    @Async
    public void syncAfterStorageSpaceCreated(Long storageSpaceId) {
        try {
            rentalUnitSyncService.createForStorageSpace(storageSpaceId);
        } catch (Exception e) {
            log.error("Failed to automatically synchronize RentalUnit for StorageSpace ID: {}", storageSpaceId, e);
        }
    }
}
