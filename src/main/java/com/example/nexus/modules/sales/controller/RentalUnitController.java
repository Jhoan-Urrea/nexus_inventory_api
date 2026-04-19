package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.request.UpdateRentalUnitPricingRequestDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitPricingDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitWarehouseCatalogCardDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.mapper.RentalUnitMapper;
import com.example.nexus.modules.sales.service.RentalAvailabilityService;
import com.example.nexus.modules.sales.service.RentalUnitCatalogService;
import com.example.nexus.modules.sales.service.RentalUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales/rental-units")
@RequiredArgsConstructor
@Tag(name = "Sales Rental Units", description = "Basic CRUD for rental units")
@SecurityRequirement(name = "bearerAuth")
public class RentalUnitController {

    private final RentalUnitService rentalUnitService;
    private final RentalUnitCatalogService rentalUnitCatalogService;
    private final RentalUnitMapper rentalUnitMapper;
    private final RentalAvailabilityService rentalAvailabilityService;

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
    @Operation(summary = "List rental units with optional availability status by date range")
    public ResponseEntity<List<RentalUnitDTO>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate != null || endDate != null) {
            return ResponseEntity.ok(rentalAvailabilityService.getCatalogWithAvailability(startDate, endDate));
        }
        return ResponseEntity.ok(
                rentalUnitService.findAll().stream()
                        .map(rentalUnitMapper::toDto)
                        .toList()
        );
    }

    @GetMapping("/pricing")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "List pricing catalog for rental units")
    public ResponseEntity<List<RentalUnitPricingDTO>> findPricingCatalog(
            @RequestParam(required = false) Boolean readyOnly,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ResponseEntity.ok(rentalUnitService.findPricingCatalog(readyOnly, activeOnly));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Get rental unit by id")
    public ResponseEntity<RentalUnitDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalUnitMapper.toDto(rentalUnitService.findById(id)));
    }

    @GetMapping("/{id:\\d+}/catalog-card")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(
            summary = "Ficha de catálogo (bodega completa)",
            description = "Solo para rental units tipo WAREHOUSE sin sector ni espacio enlazado. "
                    + "Incluye datos descriptivos de la bodega y conteos de sectores y espacios registrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha obtenida"),
            @ApiResponse(responseCode = "404", description = "Unidad inexistente o no es oferta de bodega completa")
    })
    public ResponseEntity<RentalUnitWarehouseCatalogCardDTO> getWarehouseCatalogCard(@PathVariable Long id) {
        return ResponseEntity.ok(rentalUnitCatalogService.getWarehouseCatalogCard(id));
    }

    @PatchMapping("/{id:\\d+}/pricing")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configure pricing for a rental unit (admin)")
    public ResponseEntity<RentalUnitPricingDTO> updatePricing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRentalUnitPricingRequestDTO request,
            Authentication authentication
    ) {
        String actorEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(rentalUnitService.updatePricing(id, request, actorEmail));
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Update rental unit")
    public ResponseEntity<RentalUnitDTO> update(@PathVariable Long id, @RequestBody RentalUnit rentalUnit) {
        return ResponseEntity.ok(rentalUnitMapper.toDto(rentalUnitService.update(id, rentalUnit)));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Delete rental unit")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rentalUnitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
