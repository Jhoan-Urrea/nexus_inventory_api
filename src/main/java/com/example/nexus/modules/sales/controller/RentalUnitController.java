package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.response.RentalUnitDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.mapper.RentalUnitMapper;
import com.example.nexus.modules.sales.service.RentalUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales/rental-units")
@RequiredArgsConstructor
@Tag(name = "Sales Rental Units", description = "Basic CRUD for rental units")
@SecurityRequirement(name = "bearerAuth")
public class RentalUnitController {

    private final RentalUnitService rentalUnitService;
    private final RentalUnitMapper rentalUnitMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Create rental unit")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rental unit created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RentalUnitDTO> create(@RequestBody RentalUnit rentalUnit) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalUnitMapper.toDto(rentalUnitService.create(rentalUnit)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "List rental units")
    public ResponseEntity<List<RentalUnitDTO>> findAll() {
        return ResponseEntity.ok(
                rentalUnitService.findAll().stream()
                        .map(rentalUnitMapper::toDto)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Get rental unit by id")
    public ResponseEntity<RentalUnitDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalUnitMapper.toDto(rentalUnitService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Update rental unit")
    public ResponseEntity<RentalUnitDTO> update(@PathVariable Long id, @RequestBody RentalUnit rentalUnit) {
        return ResponseEntity.ok(rentalUnitMapper.toDto(rentalUnitService.update(id, rentalUnit)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Delete rental unit")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rentalUnitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
