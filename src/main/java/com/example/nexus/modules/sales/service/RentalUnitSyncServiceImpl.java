package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalUnitSyncServiceImpl implements RentalUnitSyncService {

    @Value("${app.sales.rental-unit.default-currency:COP}")
    private String defaultCurrency;

    private final RentalUnitRepository rentalUnitRepository;
    private final EntityTypeRepository entityTypeRepository;
    
    private final WarehouseRepository warehouseRepository;
    private final SectorRepository sectorRepository;
    private final StorageSpaceRepository storageSpaceRepository;

    private static final String TYPE_WAREHOUSE = "WAREHOUSE";
    private static final String TYPE_SECTOR = "SECTOR";
    private static final String TYPE_STORAGE_SPACE = "STORAGE_SPACE";

    /**
     * REQUIRES_NEW: commit independiente del listener (el warehouse ya esta persistido).
     * Requiere {@code spring.datasource.hikari.maximum-pool-size >= 2} en dev por el callback post-commit.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForWarehouse(Long warehouseId) {
        if (!rentalUnitRepository.existsByWarehouseId(warehouseId)) {
            EntityType entityType = getEntityType(TYPE_WAREHOUSE);
            if (entityType != null) {
                RentalUnit rentalUnit = RentalUnit.builder()
                        .warehouse(warehouseRepository.getReferenceById(warehouseId))
                        .entityType(entityType)
                        .currency(normalizeCurrency(defaultCurrency))
                        .priceActive(Boolean.FALSE)
                        .build();
                rentalUnitRepository.save(rentalUnit);
                rentalUnitRepository.flush();
                log.info("RentalUnit persisted rentalUnitId={} warehouseId={}", rentalUnit.getId(), warehouseId);
            } else {
                log.warn("EntityType {} not found, skipping RentalUnit creation", TYPE_WAREHOUSE);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForSector(Long sectorId) {
        if (!rentalUnitRepository.existsBySectorId(sectorId)) {
            EntityType entityType = getEntityType(TYPE_SECTOR);
            if (entityType != null) {
                RentalUnit rentalUnit = RentalUnit.builder()
                        .sector(sectorRepository.getReferenceById(sectorId))
                        .entityType(entityType)
                        .currency(normalizeCurrency(defaultCurrency))
                        .priceActive(Boolean.FALSE)
                        .build();
                rentalUnitRepository.save(rentalUnit);
                rentalUnitRepository.flush();
                log.info("RentalUnit persisted rentalUnitId={} sectorId={}", rentalUnit.getId(), sectorId);
            } else {
                log.warn("EntityType {} not found, skipping RentalUnit creation", TYPE_SECTOR);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForStorageSpace(Long storageSpaceId) {
        if (!rentalUnitRepository.existsByStorageSpaceId(storageSpaceId)) {
            EntityType entityType = getEntityType(TYPE_STORAGE_SPACE);
            if (entityType != null) {
                RentalUnit rentalUnit = RentalUnit.builder()
                        .storageSpace(storageSpaceRepository.getReferenceById(storageSpaceId))
                        .entityType(entityType)
                        .currency(normalizeCurrency(defaultCurrency))
                        .priceActive(Boolean.FALSE)
                        .build();
                rentalUnitRepository.save(rentalUnit);
                rentalUnitRepository.flush();
                log.info("RentalUnit persisted rentalUnitId={} storageSpaceId={}", rentalUnit.getId(), storageSpaceId);
            } else {
                log.warn("EntityType {} not found, skipping RentalUnit creation", TYPE_STORAGE_SPACE);
            }
        }
    }

    @Override
    @Transactional
    public void syncAll() {
        log.info("Starting bulk synchronization of physical inventory to RentalUnits...");
        
        List<Warehouse> warehouses = warehouseRepository.findAll();
        for (Warehouse warehouse : warehouses) {
            createForWarehouse(warehouse.getId());
        }

        List<Sector> sectors = sectorRepository.findAll();
        for (Sector sector : sectors) {
            createForSector(sector.getId());
        }

        List<StorageSpace> spaces = storageSpaceRepository.findAll();
        for (StorageSpace space : spaces) {
            createForStorageSpace(space.getId());
        }
        
        log.info("Bulk synchronization of RentalUnits completed.");
    }

    private EntityType getEntityType(String name) {
        return entityTypeRepository.findByName(name).orElseGet(() -> {
            log.error("Missing EntityType definition in database for name: {}", name);
            return null;
        });
    }

    private static String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return "COP";
        }
        return raw.trim().toUpperCase();
    }
}
