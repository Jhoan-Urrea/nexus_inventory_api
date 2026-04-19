package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.service.RentalUnitSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/rental-units/sync")
@RequiredArgsConstructor
@Tag(name = "Sales Rental Units Sync", description = "Administrative operations to sync physical inventory to rental units")
@SecurityRequirement(name = "bearerAuth")
public class RentalUnitSyncController {

    private final RentalUnitSyncService rentalUnitSyncService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Synchronize all existing physical units to RentalUnits")
    public ResponseEntity<String> syncAll() {
        rentalUnitSyncService.syncAll();
        return ResponseEntity.ok("Successfully synchronized all physical units to RentalUnits.");
    }
}
