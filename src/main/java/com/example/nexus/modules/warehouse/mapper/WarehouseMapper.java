package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.WarehouseResponse;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);
}
