package com.example.nexus.modules.sales.event;

import com.example.nexus.modules.warehouse.event.SectorCreatedEvent;
import com.example.nexus.modules.warehouse.event.StorageSpaceCreatedEvent;
import com.example.nexus.modules.warehouse.event.WarehouseCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalUnitSyncEventListener {

    private final RentalUnitSyncAsyncExecutor asyncExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onWarehouseCreated(WarehouseCreatedEvent event) {
        Long warehouseId = event.getWarehouseId();
        log.info("Received WarehouseCreatedEvent for Warehouse ID: {}, scheduling async sync", warehouseId);
        asyncExecutor.syncAfterWarehouseCreated(warehouseId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSectorCreated(SectorCreatedEvent event) {
        Long sectorId = event.getSectorId();
        log.info("Received SectorCreatedEvent for Sector ID: {}, scheduling async sync", sectorId);
        asyncExecutor.syncAfterSectorCreated(sectorId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStorageSpaceCreated(StorageSpaceCreatedEvent event) {
        Long storageSpaceId = event.getStorageSpaceId();
        log.info("Received StorageSpaceCreatedEvent for StorageSpace ID: {}, scheduling async sync", storageSpaceId);
        asyncExecutor.syncAfterStorageSpaceCreated(storageSpaceId);
    }
}
