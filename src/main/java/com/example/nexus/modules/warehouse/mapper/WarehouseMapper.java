package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.CreateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.UpdateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.WarehouseResponse;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    Warehouse toEntity(CreateWarehouseRequest request);

    Warehouse toEntity(UpdateWarehouseRequest request);

    WarehouseResponse toResponse(Warehouse warehouse);
}
