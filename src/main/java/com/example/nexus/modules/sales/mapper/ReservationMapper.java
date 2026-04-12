package com.example.nexus.modules.sales.mapper;

import com.example.nexus.modules.sales.dto.request.CreateReservationRequestDTO;
import com.example.nexus.modules.sales.dto.response.ReservationResponseDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.user.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final RentalUnitMapper rentalUnitMapper;

    public Reservation toEntity(
            CreateReservationRequestDTO dto,
            Client client,
            RentalUnit rentalUnit,
            String reservationToken,
            Integer status,
            LocalDateTime expiresAt
    ) {
        return Reservation.builder()
                .client(client)
                .rentalUnit(rentalUnit)
                .reservationToken(reservationToken)
                .status(status)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .expiresAt(expiresAt)
                .build();
    }

    public ReservationResponseDTO toResponseDTO(Reservation entity) {
        return new ReservationResponseDTO(
                entity.getId(),
                entity.getClient() != null ? entity.getClient().getId() : null,
                entity.getClient() != null ? entity.getClient().getName() : null,
                entity.getRentalUnit() != null ? rentalUnitMapper.toResponseDTO(entity.getRentalUnit()) : null,
                entity.getReservationToken(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}
