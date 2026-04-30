package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.response.EntityTypeResponseDTO;

import java.util.List;

public interface EntityTypeService {
    EntityTypeResponseDTO create(CreateEntityTypeRequestDTO dto);
    List<EntityTypeResponseDTO> findAll();
    EntityTypeResponseDTO findById(Long id);
    EntityTypeResponseDTO update(Long id, UpdateEntityTypeRequestDTO dto);
    void delete(Long id);
}
