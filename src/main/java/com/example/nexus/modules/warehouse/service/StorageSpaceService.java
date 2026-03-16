package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceResponseDTO;

import java.util.List;

public interface StorageSpaceService {
    StorageSpaceResponseDTO create(CreateStorageSpaceRequestDTO dto);
    StorageSpaceResponseDTO findByCode(String code);
    List<StorageSpaceResponseDTO> findBySectorId(Long sectorId);
    StorageSpaceResponseDTO update(Long id, UpdateStorageSpaceRequestDTO dto);
    void delete(Long id);
}
