package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceTypeResponseDTO;

import java.util.List;

public interface StorageSpaceTypeService {

    List<StorageSpaceTypeResponseDTO> findAll();

    StorageSpaceTypeResponseDTO create( CreateStorageSpaceTypeRequestDTO dto);

}
