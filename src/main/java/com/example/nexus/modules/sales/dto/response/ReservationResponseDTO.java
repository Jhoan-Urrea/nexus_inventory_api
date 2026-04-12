package com.example.nexus.modules.sales.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record ReservationResponseDTO(
        Long reservationId,
        Long clientId,
        String clientName,
        RentalUnitResponseDTO rentalUnit,
        String reservationToken,
        Integer status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
