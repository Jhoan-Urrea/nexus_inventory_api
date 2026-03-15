package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;

import java.util.List;

public interface StatusCatalogService {
    StatusCatalogResponseDTO create(CreateStatusCatalogRequestDTO dto);
    List<StatusCatalogResponseDTO> findByEntityType(Long entityTypeId);
    StatusCatalogResponseDTO findById(Long id);
}
