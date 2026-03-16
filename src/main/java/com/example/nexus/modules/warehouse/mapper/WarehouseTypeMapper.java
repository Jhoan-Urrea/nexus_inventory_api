package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.WarehouseTypeCreateDTO;
import com.example.nexus.modules.warehouse.dto.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WarehouseTypeMapper {

    @Mapping(target = "id", ignore = true)
    WarehouseType toEntity(WarehouseTypeCreateDTO dto);

    WarehouseTypeResponseDTO toResponseDTO(WarehouseType entity);
}
