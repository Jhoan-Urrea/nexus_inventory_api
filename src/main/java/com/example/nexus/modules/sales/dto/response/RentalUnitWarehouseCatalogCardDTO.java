package com.example.nexus.modules.sales.dto.response;

import java.math.BigDecimal;

/**
 * Ficha de oferta para una {@link com.example.nexus.modules.sales.entity.RentalUnit} a nivel bodega completa
 * (tipo WAREHOUSE, sin sector ni espacio enlazado en la fila de rental_units).
 */
public record RentalUnitWarehouseCatalogCardDTO(
        Long rentalUnitId,
        String offerScope,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String warehouseLocation,
        boolean warehouseActive,
        Long cityId,
        String cityName,
        Long warehouseTypeId,
        String warehouseTypeName,
        String warehouseTypeDescription,
        BigDecimal totalWarehouseCapacityM2,
        /** Por ahora igual a capacidad total de bodega; reservado para reglas futuras (área comercializable). */
        BigDecimal offeredAreaM2,
        int registeredSectorsCount,
        int registeredStorageSpacesCount,
        String warehouseStatusCode,
        String warehouseStatusDescription,
        Boolean warehouseStatusOperational,
        /** Texto tipo "COD · Ciudad" para pie de card. */
        String locationSummaryLine,
        /** Etiqueta corta para cabecera, p. ej. "Bodega · COD". */
        String unitTitleLabel
) {}
