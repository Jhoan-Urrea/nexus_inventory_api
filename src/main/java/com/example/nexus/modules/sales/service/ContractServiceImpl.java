package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreateContractRentalUnitRequestDTO;
import com.example.nexus.modules.sales.dto.request.CreateContractRequest;
import com.example.nexus.modules.sales.dto.request.RentalAvailabilityRequestDTO;
import com.example.nexus.modules.sales.dto.response.RentalAvailabilityResponseDTO;
import com.example.nexus.modules.sales.dto.response.ContractResponseDTO;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.sales.entity.ReservationStatus;
import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.sales.mapper.ContractMapper;
import com.example.nexus.modules.sales.mapper.ContractRentalUnitMapper;
import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.sales.security.OwnershipValidationService;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractServiceImpl implements ContractService {

    private static final int RESERVATION_STATUS_PENDING = ReservationStatus.PENDING.getCode();
    private static final int RESERVATION_STATUS_CANCELLED = ReservationStatus.CANCELLED.getCode();
    private static final int RESERVATION_STATUS_EXPIRED = ReservationStatus.EXPIRED.getCode();
    private static final int RESERVATION_STATUS_CONFIRMED = ReservationStatus.CONFIRMED.getCode();
    private static final String MSG_CONTRACT_NOT_FOUND = "Contrato no encontrado";
    private static final String MSG_RENTAL_UNIT_NOT_FOUND = "Unidad de renta no encontrada";
    private static final String MSG_RESERVATION_NOT_FOUND = "Reserva no encontrada";
    private static final String MSG_RENTAL_UNIT_PRICE_NOT_CONFIGURED = "La unidad de renta no tiene precio base configurado";
    private static final String MSG_RENTAL_UNIT_PRICE_INACTIVE = "La unidad de renta tiene precio inactivo";

    private final ContractRepository contractRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final ContractMapper contractMapper;
    private final ContractRentalUnitMapper contractRentalUnitMapper;
    private final RentalAvailabilityService rentalAvailabilityService;
    private final OwnershipValidationService ownershipValidationService;
    private final ContractStateMachineService contractStateMachineService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponseDTO createContract(CreateContractRequest request) {
        validateDateRange(request.startDate(), request.endDate(), "El rango de fechas del contrato no es valido");

        boolean isDirectContract = request.reservationToken() == null || request.reservationToken().isBlank();

        if (isDirectContract) {
            return createDirectContract(request);
        } else {
            return createContractFromReservation(request);
        }
    }

    private ContractResponseDTO createDirectContract(CreateContractRequest request) {
        if (request.clientId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se requiere clientId para un contrato directo sin reserva"
            );
        }
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Contract contract = contractMapper.toEntity(request, client);

        List<CreateContractRentalUnitRequestDTO> orderedItems = request.contractRentalUnits().stream()
                .sorted(java.util.Comparator.comparing(CreateContractRentalUnitRequestDTO::rentalUnitId))
                .toList();

        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (CreateContractRentalUnitRequestDTO item : orderedItems) {
            if (!seen.add(item.rentalUnitId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No se puede asignar la misma unidad de renta mas de una vez al contrato"
                );
            }
            validateRentalUnitWithinContract(contract.getStartDate(), contract.getEndDate(), item);
            RentalUnit rentalUnit = requireRentalUnitForUpdate(item.rentalUnitId());
            validateRentalUnitAvailabilityForContract(item, null, rentalUnit);
            BigDecimal resolvedPrice = resolveCatalogPrice(rentalUnit);
            contract.addRentalUnit(contractRentalUnitMapper.toEntity(item, rentalUnit, resolvedPrice));
        }

        contract.setTotalAmount(calculateTotalContractPrice(contract.getRentalUnits()));
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    private ContractResponseDTO createContractFromReservation(CreateContractRequest request) {
        Reservation reservation = requireConvertibleReservation(request.reservationToken());
        ownershipValidationService.validateReservationOwnership(reservation.getId(), currentAuthentication());
        validateRequestedRentalUnits(request, reservation);

        Contract contract = contractMapper.toEntity(request, reservation.getClient());

        List<CreateContractRentalUnitRequestDTO> orderedItems = request.contractRentalUnits().stream()
                .sorted(java.util.Comparator.comparing(CreateContractRentalUnitRequestDTO::rentalUnitId))
                .toList();

        for (CreateContractRentalUnitRequestDTO item : orderedItems) {
            validateRentalUnitWithinContract(contract.getStartDate(), contract.getEndDate(), item);

            RentalUnit rentalUnit = requireRentalUnitForUpdate(item.rentalUnitId());
            validateRentalUnitAvailabilityForContract(item, reservation, rentalUnit);
            BigDecimal resolvedPrice = resolveCatalogPrice(rentalUnit);

            ContractRentalUnit contractRentalUnit = contractRentalUnitMapper.toEntity(item, rentalUnit, resolvedPrice);
            contract.addRentalUnit(contractRentalUnit);
        }

        contract.setTotalAmount(calculateTotalContractPrice(contract.getRentalUnits()));
        Contract savedContract = contractRepository.save(contract);

        reservation.setStatus(RESERVATION_STATUS_CONFIRMED);
        reservation.setExpiresAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        return contractMapper.toResponseDTO(savedContract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponseDTO findById(Long contractId) {
        ownershipValidationService.validateContractOwnership(contractId, currentAuthentication());
        return contractRepository.findByIdWithAssociations(contractId)
                .map(contractMapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponseDTO> findAll() {
        List<Contract> contracts = ownershipValidationService.hasElevatedSalesAccess(currentAuthentication())
                ? contractRepository.findAllWithAssociations()
                : contractRepository.findAllByClientIdWithAssociations(
                        ownershipValidationService.requireClientId(currentAuthentication())
                );
        return contracts.stream()
                .map(contractMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponseDTO> findMyActiveContracts() {
        Long clientId = ownershipValidationService.requireClientId(currentAuthentication());
        List<Contract> contracts = contractRepository.findAllByClientIdAndStatusWithAssociations(
                clientId,
                ContractStatus.ACTIVE.getCode()
        );
        return contracts.stream()
                .map(contractMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponseDTO completeContract(Long contractId) {
        ownershipValidationService.validateContractOwnership(contractId, currentAuthentication());
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
        contractStateMachineService.transitionStatus(contract, ContractStatus.COMPLETED);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractResponseDTO cancelContract(Long contractId) {
        ownershipValidationService.validateContractOwnership(contractId, currentAuthentication());
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
        contractStateMachineService.transitionStatus(contract, ContractStatus.CANCELLED);
        return contractMapper.toResponseDTO(contractRepository.save(contract));
    }

    private RentalUnit requireRentalUnitForUpdate(Long rentalUnitId) {
        return rentalUnitRepository.findByIdForUpdate(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_RENTAL_UNIT_NOT_FOUND));
    }

    private Reservation requireConvertibleReservation(String reservationToken) {
        Reservation reservation = reservationRepository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == RESERVATION_STATUS_CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya fue cancelada");
        }
        if (reservation.getStatus() == RESERVATION_STATUS_CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya fue confirmada");
        }
        if (reservation.getStatus() == RESERVATION_STATUS_EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya expiro");
        }
        if (reservation.getStatus() != RESERVATION_STATUS_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva no se encuentra activa");
        }
        if (reservation.getExpiresAt() != null && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            reservation.setStatus(RESERVATION_STATUS_EXPIRED);
            reservationRepository.save(reservation);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya expiro");
        }

        return reservation;
    }

    private void validateRequestedRentalUnits(CreateContractRequest request, Reservation reservation) {
        Set<Long> seenRentalUnitIds = new HashSet<>();
        boolean includesReservedRentalUnit = false;

        List<Long> reservedUnitIds = reservation.getRentalUnits().stream()
                .map(rru -> rru.getRentalUnit().getId())
                .toList();

        for (CreateContractRentalUnitRequestDTO rentalUnitRequest : request.contractRentalUnits()) {
            if (!seenRentalUnitIds.add(rentalUnitRequest.rentalUnitId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No se puede asignar la misma unidad de renta mas de una vez al contrato"
                );
            }
            if (reservedUnitIds.contains(rentalUnitRequest.rentalUnitId())) {
                includesReservedRentalUnit = true;
            }
        }

        if (!includesReservedRentalUnit) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El contrato debe incluir al menos una unidad de renta asociada a la reserva"
            );
        }
    }

    private void validateRentalUnitAvailabilityForContract(
            CreateContractRentalUnitRequestDTO request,
            Reservation reservation,
            RentalUnit rentalUnit
    ) {
        Long excludedReservationId = null;
        if (reservation != null) {
            boolean isReserved = reservation.getRentalUnits().stream()
                    .anyMatch(rru -> rru.getRentalUnit().getId().equals(rentalUnit.getId()));
            excludedReservationId = isReserved ? reservation.getId() : null;
        }

        RentalAvailabilityResponseDTO availability = rentalAvailabilityService.validate(
                new RentalAvailabilityRequestDTO(
                        rentalUnit.getId(),
                        request.startDate(),
                        request.endDate(),
                        excludedReservationId,
                        null
                )
        );

        if (Boolean.FALSE.equals(availability.available())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, availability.message());
        }
    }

    private BigDecimal calculateTotalContractPrice(List<ContractRentalUnit> rentalUnits) {
        return rentalUnits.stream()
                .map(ContractRentalUnit::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveCatalogPrice(RentalUnit rentalUnit) {
        if (Boolean.FALSE.equals(rentalUnit.getPriceActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_RENTAL_UNIT_PRICE_INACTIVE);
        }
        if (rentalUnit.getBasePrice() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_RENTAL_UNIT_PRICE_NOT_CONFIGURED);
        }
        if (rentalUnit.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_RENTAL_UNIT_PRICE_NOT_CONFIGURED);
        }
        return rentalUnit.getBasePrice();
    }

    private void validateRentalUnitWithinContract(
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            CreateContractRentalUnitRequestDTO dto
    ) {
        validateDateRange(dto.startDate(), dto.endDate(), "El rango de fechas de la unidad rentada no es valido");

        if (dto.startDate().isBefore(contractStartDate) || dto.endDate().isAfter(contractEndDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las fechas de la unidad rentada deben estar contenidas dentro del contrato"
            );
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String message) {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
