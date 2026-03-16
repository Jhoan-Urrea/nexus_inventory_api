package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;

import java.util.List;

public interface MaintenanceOrderService {
    MaintenanceOrderResponseDTO create(CreateMaintenanceOrderRequestDTO dto);
    MaintenanceOrderResponseDTO completeOrder(Long id);
    List<MaintenanceOrderResponseDTO> findAll();
}
