package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.response.ContractRentalUnitResponseDTO;
import com.example.nexus.modules.sales.service.ContractRentalUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales/contract-rental-units")
@RequiredArgsConstructor
@Tag(name = "Sales Contract Rental Units", description = "Linked rental units inside contracts")
@SecurityRequirement(name = "bearerAuth")
public class ContractRentalUnitController {

    private final ContractRentalUnitService contractRentalUnitService;

    @GetMapping("/{contractRentalUnitId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Get contract rental unit by id")
    public ResponseEntity<ContractRentalUnitResponseDTO> findById(@PathVariable Long contractRentalUnitId) {
        return ResponseEntity.ok(contractRentalUnitService.findById(contractRentalUnitId));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "List rental units by contract")
    public ResponseEntity<List<ContractRentalUnitResponseDTO>> findByContractId(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractRentalUnitService.findByContractId(contractId));
    }
}
