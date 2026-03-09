package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.CreateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.UpdateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.WarehouseResponse;

import java.util.List;

public interface WarehouseService {

    WarehouseResponse createWarehouse(CreateWarehouseRequest request);

    List<WarehouseResponse> getAllWarehouses();

    WarehouseResponse getWarehouseById(Long id);

    WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request);

    void deleteWarehouse(Long id);
}

