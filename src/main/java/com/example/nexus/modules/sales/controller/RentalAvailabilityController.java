package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.request.RentalAvailabilityRequestDTO;
import com.example.nexus.modules.sales.dto.request.RentalBulkAvailabilityRequestDTO;
import com.example.nexus.modules.sales.dto.response.AvailabilitySummaryResponseDTO;
import com.example.nexus.modules.sales.dto.response.RentalAvailabilityResponseDTO;
import com.example.nexus.modules.sales.service.RentalAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/sales/availability", "/sales/availability"})
@RequiredArgsConstructor
@Tag(name = "Sales Availability", description = "Availability validation for rental units")
@SecurityRequirement(name = "bearerAuth")
public class RentalAvailabilityController {

    private final RentalAvailabilityService rentalAvailabilityService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Validate rental unit availability")
    public ResponseEntity<RentalAvailabilityResponseDTO> validate(@Valid @RequestBody RentalAvailabilityRequestDTO request) {
        return ResponseEntity.ok(rentalAvailabilityService.validate(request));
    }

    @PostMapping("/validate-bulk")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Validate availability for multiple rental units at once (pre-checkout)")
    public ResponseEntity<Boolean> validateBulk(@Valid @RequestBody RentalBulkAvailabilityRequestDTO request) {
        return ResponseEntity.ok(rentalAvailabilityService.validateBulk(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Get availability summary by scope")
    public ResponseEntity<AvailabilitySummaryResponseDTO> getAvailability(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) Long storageSpaceId
    ) {
        return ResponseEntity.ok(
                rentalAvailabilityService.getAvailabilitySummary(warehouseId, sectorId, storageSpaceId)
        );
    }
}
