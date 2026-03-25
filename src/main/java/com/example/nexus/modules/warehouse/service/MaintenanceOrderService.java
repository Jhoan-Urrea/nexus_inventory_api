package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;

import java.util.List;

public interface MaintenanceOrderService {
    MaintenanceOrderResponseDTO create(CreateMaintenanceOrderRequestDTO dto);
    MaintenanceOrderResponseDTO completeOrder(Long id);
    MaintenanceOrderResponseDTO findById(Long id);
    List<MaintenanceOrderResponseDTO> findAll();
    MaintenanceOrderResponseDTO update(Long id, UpdateMaintenanceOrderRequestDTO dto);
    void delete(Long id);
}
