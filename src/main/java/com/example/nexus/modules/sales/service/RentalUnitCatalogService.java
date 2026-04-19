package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.response.RentalUnitWarehouseCatalogCardDTO;

public interface RentalUnitCatalogService {

    /**
     * Ficha descriptiva para oferta de bodega completa.
     *
     * @throws org.springframework.web.server.ResponseStatusException 404 si no existe la unidad
     *         o no es de alcance bodega completa (tipo WAREHOUSE y sin sector/espacio en rental_units).
     */
    RentalUnitWarehouseCatalogCardDTO getWarehouseCatalogCard(Long rentalUnitId);
}
