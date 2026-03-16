package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.dto.WarehouseTypeCreateDTO;
import com.example.nexus.modules.warehouse.dto.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.service.WarehouseTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.RequestMapping;
import org.springframework.web.bind.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/warehouse-types")
@RequiredArgsConstructor
public class WarehouseTypeController {

    private final WarehouseTypeService warehouseTypeService;

    @PostMapping
    public ResponseEntity<WarehouseTypeResponseDTO> createWarehouseType(
            @Valid @RequestBody WarehouseTypeCreateDTO createDTO) {
        
        WarehouseTypeResponseDTO responseDTO = warehouseTypeService.createWarehouseType(createDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }
}
