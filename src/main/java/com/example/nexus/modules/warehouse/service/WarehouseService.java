package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseResponseDTO;

import java.util.List;


public interface WarehouseService {
    WarehouseResponseDTO create(CreateWarehouseRequestDTO dto);
    WarehouseResponseDTO update(Long id, UpdateWarehouseRequestDTO dto);
    WarehouseResponseDTO findById(Long id);
    List<WarehouseResponseDTO> findAll();
    WarehouseResponseDTO delete(Long id);
    WarehouseResponseDTO disable(Long id);

    /** Reactiva la bodega ({@code active=true}) y alinea el estado del catálogo a uno operacional, si existe. */
    WarehouseResponseDTO enable(Long id);
}

