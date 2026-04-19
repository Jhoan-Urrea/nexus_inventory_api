package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreateReservationRequestDTO;
import com.example.nexus.modules.sales.dto.response.ReservationResponseDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.sales.entity.ReservationRentalUnit;
import com.example.nexus.modules.sales.entity.ReservationStatus;
import com.example.nexus.modules.sales.mapper.ReservationMapper;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.sales.security.OwnershipValidationService;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final int STATUS_PENDING = ReservationStatus.PENDING.getCode();
    private static final int STATUS_CANCELLED = ReservationStatus.CANCELLED.getCode();
    private static final int STATUS_EXPIRED = ReservationStatus.EXPIRED.getCode();
    private static final String MSG_CLIENT_NOT_FOUND = "Cliente no encontrado";
    private static final String MSG_RENTAL_UNIT_NOT_FOUND = "Unidad de renta no encontrada";
    private static final String MSG_RESERVATION_NOT_FOUND = "Reserva no encontrada";
    private static final String MSG_RESERVATION_TOKEN_NOT_FOUND = "Reserva no encontrada para el token suministrado";

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final ReservationMapper reservationMapper;
    private final RentalAvailabilityService rentalAvailabilityService;
    private final OwnershipValidationService ownershipValidationService;
    private final AvailabilityPolicyService availabilityPolicyService;
    private final ReservationCreatedClientEmailService reservationCreatedClientEmailService;

    @Value("${sales.reservation.expiration-minutes:30}")
    private long reservationExpirationMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponseDTO createReservation(CreateReservationRequestDTO dto) {
        Client client = requireClient(dto.clientId());
        validateReservationDateRange(dto.startDate(), dto.endDate());

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationExpirationMinutes);
        Reservation reservation = reservationMapper.toEntity(
                dto,
                client,
                generateReservationToken(),
                STATUS_PENDING,
                expiresAt
        );

        List<Long> distinctSortedIds = dto.rentalUnitIds().stream().distinct().sorted().toList();
        for (Long unitId : distinctSortedIds) {
            RentalUnit rentalUnit = requireRentalUnitForUpdate(unitId);
            validateAvailabilityForReservation(rentalUnit.getId(), dto.startDate(), dto.endDate());
            
            ReservationRentalUnit rru = ReservationRentalUnit.builder()
                    .rentalUnit(rentalUnit)
                    .build();
            reservation.addRentalUnit(rru);
        }

        try {
            Reservation saved = reservationRepository.save(reservation);
            reservationCreatedClientEmailService.sendReservationCreatedEmail(saved.getId());
            return reservationMapper.toResponseDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation token duplicado", ex);
        }
    }

    @Override
    public ReservationResponseDTO findById(Long reservationId) {
        ownershipValidationService.validateReservationOwnership(reservationId, currentAuthentication());
        return reservationRepository.findByIdWithAssociations(reservationId)
                .map(this::normalizeReservationState)
                .map(reservationMapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RESERVATION_NOT_FOUND));
    }

    @Override
    public List<ReservationResponseDTO> findAll() {
        List<Reservation> reservations = ownershipValidationService.hasElevatedSalesAccess(currentAuthentication())
                ? reservationRepository.findAllByOrderByCreatedAtDesc()
                : reservationRepository.findAllByClientIdOrderByCreatedAtDesc(
                        ownershipValidationService.requireClientId(currentAuthentication())
                );
        return reservations.stream()
                .map(this::normalizeReservationState)
                .map(reservationMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponseDTO cancelReservation(String reservationToken) {
        Reservation reservation = normalizeReservationState(requireReservationByToken(reservationToken));
        ownershipValidationService.validateReservationOwnership(reservation.getId(), currentAuthentication());

        if (reservation.getStatus() == STATUS_PENDING) {
            reservation.setStatus(STATUS_CANCELLED);
            reservation.setExpiresAt(LocalDateTime.now());
            reservation = reservationRepository.save(reservation);
        }

        return reservationMapper.toResponseDTO(reservation);
    }

    @Override
    public ReservationResponseDTO getReservationByToken(String reservationToken) {
        Reservation reservation = normalizeReservationState(requireReservationByToken(reservationToken));
        ownershipValidationService.validateReservationOwnership(reservation.getId(), currentAuthentication());
        return reservationMapper.toResponseDTO(reservation);
    }

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_CLIENT_NOT_FOUND));
    }

    private RentalUnit requireRentalUnit(Long rentalUnitId) {
        return rentalUnitRepository.findByIdWithAssociations(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_RENTAL_UNIT_NOT_FOUND));
    }

    private RentalUnit requireRentalUnitForUpdate(Long rentalUnitId) {
        return rentalUnitRepository.findByIdForUpdate(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_RENTAL_UNIT_NOT_FOUND));
    }

    private Reservation requireReservationByToken(String reservationToken) {
        return reservationRepository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RESERVATION_TOKEN_NOT_FOUND));
    }

    private void validateAvailabilityForReservation(Long rentalUnitId, LocalDate startDate, LocalDate endDate) {
        rentalAvailabilityService.validateReservationAvailability(rentalUnitId, null);

        boolean hasReservationConflict = availabilityPolicyService.hasReservationConflict(
                rentalUnitId,
                LocalDateTime.now(),
                null,
                startDate,
                endDate
        );
        if (hasReservationConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La unidad no se encuentra disponible para reservar");
        }

        boolean hasContractConflict = availabilityPolicyService.evaluateContractAvailability(
                rentalUnitId,
                startDate,
                endDate,
                null
        ).hasConflict();
        if (hasContractConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La unidad ya tiene un contrato en el rango solicitado");
        }
    }

    private String generateReservationToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (reservationRepository.existsByReservationToken(token));
        return token;
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Reservation normalizeReservationState(Reservation reservation) {
        if (reservation.getStatus() == STATUS_PENDING
                && reservation.getExpiresAt() != null
                && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            reservation.setStatus(STATUS_EXPIRED);
            return reservationRepository.save(reservation);
        }
        return reservation;
    }

    private void validateReservationDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate) || endDate.equals(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de fechas de la reserva no es valido");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser hoy o posterior");
        }
    }

    @Scheduled(fixedDelayString = "${sales.reservation.expiration-job-delay-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> toExpire = new ArrayList<>(
                reservationRepository.findByStatusAndExpiresAtBefore(STATUS_PENDING, now)
        );

        if (toExpire.isEmpty()) {
            return;
        }

        for (Reservation reservation : toExpire) {
            reservation.setStatus(STATUS_EXPIRED);
        }
        reservationRepository.saveAll(toExpire);
    }
}
