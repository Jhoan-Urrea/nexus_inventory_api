package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.WarehouseTypeCreateDTO;
import com.example.nexus.modules.warehouse.dto.WarehouseTypeResponseDTO;

public interface WarehouseTypeService {
    WarehouseTypeResponseDTO createWarehouseType(WarehouseTypeCreateDTO createDTO);
}
