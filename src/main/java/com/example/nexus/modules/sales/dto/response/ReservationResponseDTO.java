package com.example.nexus.modules.sales.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public record ReservationResponseDTO(
        Long reservationId,
        Long clientId,
        String clientName,
        List<RentalUnitResponseDTO> rentalUnits,
        String reservationToken,
        Integer status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
