package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.Warehouse;

public interface RentalUnitSyncService {
    void createForWarehouse(Long warehouseId);
    void createForSector(Long sectorId);
    void createForStorageSpace(Long storageSpaceId);
    void syncAll();
}
