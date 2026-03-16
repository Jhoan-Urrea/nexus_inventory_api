package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.SectorResponseDTO;

import java.util.List;

public interface SectorService {
    SectorResponseDTO create(CreateSectorRequestDTO dto);
    List<SectorResponseDTO> findByWarehouseId(Long warehouseId);
    SectorResponseDTO update(Long id, UpdateSectorRequestDTO dto);
    void delete(Long id);
}