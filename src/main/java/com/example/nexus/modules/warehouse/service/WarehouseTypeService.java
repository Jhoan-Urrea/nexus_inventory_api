package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseTypeResponseDTO;

import java.util.List;

public interface WarehouseTypeService {

    WarehouseTypeResponseDTO create(CreateWarehouseTypeRequestDTO dto);

    WarehouseTypeResponseDTO update(Long id, UpdateWarehouseTypeRequestDTO dto);

    WarehouseTypeResponseDTO findById(Long id);

    List<WarehouseTypeResponseDTO> findAll();

    void delete(Long id);
}
