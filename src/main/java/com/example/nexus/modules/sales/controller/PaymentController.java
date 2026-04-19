package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO;
import com.example.nexus.modules.sales.dto.response.PaymentResponseDTO;
import com.example.nexus.modules.sales.service.PaymentService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/sales/payments", "/sales/payments", "/payments"})
@RequiredArgsConstructor
@Tag(name = "Sales Payments", description = "Payment registration for contracts")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "Create payment")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody CreatePaymentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.registerPayment(request));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "List payments by contract")
    public ResponseEntity<List<PaymentResponseDTO>> findByContractId(@PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getPaymentsByContract(contractId));
    }
}
