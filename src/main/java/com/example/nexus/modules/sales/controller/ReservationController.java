package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.dto.request.CreateReservationRequestDTO;
import com.example.nexus.modules.sales.dto.response.ReservationResponseDTO;
import com.example.nexus.modules.sales.service.ReservationService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/sales/reservations", "/sales/reservations", "/reservations"})
@RequiredArgsConstructor
@Tag(name = "Sales Reservations", description = "Reservation management for rental units")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Create reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Rental unit not available")
    })
    public ResponseEntity<ReservationResponseDTO> create(@Valid @RequestBody CreateReservationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "List reservations")
    public ResponseEntity<List<ReservationResponseDTO>> findAll() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    @GetMapping("/id/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "Get reservation by id")
    public ResponseEntity<ReservationResponseDTO> findById(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.findById(reservationId));
    }

    @GetMapping({"/{reservationToken}", "/token/{reservationToken}"})
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT','CLIENT')")
    @Operation(summary = "Get reservation by token")
    public ResponseEntity<ReservationResponseDTO> getByToken(@PathVariable String reservationToken) {
        return ResponseEntity.ok(reservationService.getReservationByToken(reservationToken));
    }

    @PatchMapping({"/{reservationToken}/cancel", "/token/{reservationToken}/cancel"})
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(summary = "Cancel reservation")
    public ResponseEntity<ReservationResponseDTO> cancel(@PathVariable String reservationToken) {
        return ResponseEntity.ok(reservationService.cancelReservation(reservationToken));
    }
}
