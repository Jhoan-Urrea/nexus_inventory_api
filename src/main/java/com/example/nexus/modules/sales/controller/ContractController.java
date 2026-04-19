package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.request.CreateContractRequest;
import com.example.nexus.modules.sales.dto.response.ContractResponseDTO;
import com.example.nexus.modules.sales.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/sales/contracts", "/sales/contracts", "/contracts"})
@RequiredArgsConstructor
@Tag(name = "Sales Contracts", description = "Contract management for rental operations")
@SecurityRequirement(name = "bearerAuth")
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Create contract")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contract created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Rental unit not available")
    })
    public ResponseEntity<ContractResponseDTO> create(@Valid @RequestBody CreateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.createContract(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "List contracts")
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @GetMapping("/me/active")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
            summary = "Mis contratos activos con unidades",
            description = "Contratos en estado ACTIVE del cliente autenticado, con lineas y datos de bodega/sector/espacio para vistas tipo Mis bodegas."
    )
    public ResponseEntity<List<ContractResponseDTO>> findMyActiveContracts() {
        return ResponseEntity.ok(contractService.findMyActiveContracts());
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "Get contract by id")
    public ResponseEntity<ContractResponseDTO> findById(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.findById(contractId));
    }

    @PatchMapping("/{contractId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Complete contract")
    public ResponseEntity<ContractResponseDTO> complete(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.completeContract(contractId));
    }

    @PatchMapping("/{contractId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Cancel contract")
    public ResponseEntity<ContractResponseDTO> cancel(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.cancelContract(contractId));
    }
}
