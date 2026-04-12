package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreateReservationRequestDTO;
import com.example.nexus.modules.sales.dto.response.ReservationResponseDTO;

import java.util.List;

public interface ReservationService {

    ReservationResponseDTO createReservation(CreateReservationRequestDTO dto);

    ReservationResponseDTO cancelReservation(String reservationToken);

    ReservationResponseDTO getReservationByToken(String reservationToken);

    ReservationResponseDTO findById(Long reservationId);

    List<ReservationResponseDTO> findAll();
}
