package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.sales.dto.response.RentalUnitWarehouseCatalogCardDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalUnitCatalogServiceImpl implements RentalUnitCatalogService {

    private static final String OFFER_SCOPE_WAREHOUSE = "WAREHOUSE_FULL";

    private final RentalUnitRepository rentalUnitRepository;
    private final SectorRepository sectorRepository;
    private final StorageSpaceRepository storageSpaceRepository;

    @Override
    public RentalUnitWarehouseCatalogCardDTO getWarehouseCatalogCard(Long rentalUnitId) {
        RentalUnit unit = rentalUnitRepository.findWarehouseCatalogCardSource(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe una unidad de alquiler a nivel bodega completa con el id indicado"));

        Warehouse w = unit.getWarehouse();
        City city = w.getCity();
        WarehouseType type = w.getWarehouseType();
        StatusCatalog status = w.getStatus();

        long sectors = sectorRepository.countByWarehouse_Id(w.getId());
        long spaces = storageSpaceRepository.countByWarehouseId(w.getId());
        int sectorCount = sectors > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sectors;
        int spaceCount = spaces > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) spaces;

        BigDecimal capacity = w.getTotalCapacityM2();
        String code = w.getCode();
        String cityName = city != null ? city.getName() : null;
        String locationLine = buildLocationSummaryLine(code, cityName);
        String titleLabel = buildUnitTitleLabel(code, w.getName());

        return new RentalUnitWarehouseCatalogCardDTO(
                unit.getId(),
                OFFER_SCOPE_WAREHOUSE,
                w.getId(),
                code,
                w.getName(),
                w.getLocation(),
                Boolean.TRUE.equals(w.getActive()),
                city != null ? city.getId() : null,
                cityName,
                type != null ? type.getId() : null,
                type != null ? type.getName() : null,
                type != null ? type.getDescription() : null,
                capacity,
                capacity,
                sectorCount,
                spaceCount,
                status != null ? status.getCode() : null,
                status != null ? status.getDescription() : null,
                status != null ? status.getIsOperational() : null,
                locationLine,
                titleLabel
        );
    }

    private static String buildLocationSummaryLine(String warehouseCode, String cityName) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return cityName != null ? cityName : "";
        }
        if (cityName == null || cityName.isBlank()) {
            return warehouseCode;
        }
        return warehouseCode + " · " + cityName;
    }

    private static String buildUnitTitleLabel(String warehouseCode, String warehouseName) {
        if (warehouseName != null && !warehouseName.isBlank()) {
            return warehouseName;
        }
        if (warehouseCode != null && !warehouseCode.isBlank()) {
            return warehouseCode;
        }
        return "Bodega";
    }
}
