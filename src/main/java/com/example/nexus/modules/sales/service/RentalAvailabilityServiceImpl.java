package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.RentalAvailabilityRequestDTO;
import com.example.nexus.modules.sales.dto.response.AvailabilitySummaryResponseDTO;
import com.example.nexus.modules.sales.dto.response.RentalAvailabilityResponseDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.mapper.RentalUnitMapper;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalAvailabilityServiceImpl implements RentalAvailabilityService {

    private static final String MSG_RENTAL_UNIT_NOT_FOUND = "Unidad de renta no encontrada";

    private final RentalUnitRepository rentalUnitRepository;
    private final RentalUnitMapper rentalUnitMapper;
    private final AvailabilityPolicyService availabilityPolicyService;

    @Override
    public boolean isRentalUnitAvailable(Long rentalUnitId, LocalDate startDate, LocalDate endDate) {
        validateRequiredDateRange(startDate, endDate);

        RentalUnit rentalUnit = requireRentalUnit(rentalUnitId);
        return availabilityPolicyService.evaluateContractAvailability(
                rentalUnit.getId(),
                startDate,
                endDate,
                null
        ).isAvailable();
    }

    @Override
    public RentalAvailabilityResponseDTO validate(RentalAvailabilityRequestDTO request) {
        validateDateFilters(request.startDate(), request.endDate());

        RentalUnit rentalUnit = requireRentalUnit(request.rentalUnitId());
        LocalDateTime checkedAt = LocalDateTime.now();

        boolean hasActiveReservation = availabilityPolicyService.hasReservationConflict(
                rentalUnit.getId(),
                checkedAt,
                request.excludeReservationId(),
                request.startDate(),
                request.endDate()
        );

        AvailabilityPolicyService.ContractAvailabilityResult contractAvailability =
                request.startDate() != null && request.endDate() != null
                        ? availabilityPolicyService.evaluateContractAvailability(
                        rentalUnit.getId(),
                        request.startDate(),
                        request.endDate(),
                        request.excludeContractRentalUnitId()
                )
                        : AvailabilityPolicyService.ContractAvailabilityResult.available();
        boolean hasContractConflict = contractAvailability.hasConflict();

        boolean available = !hasActiveReservation && !hasContractConflict;
        String message = buildMessage(
                hasActiveReservation,
                contractAvailability,
                request.startDate(),
                request.endDate()
        );

        return new RentalAvailabilityResponseDTO(
                rentalUnitMapper.toResponseDTO(rentalUnit),
                available,
                hasActiveReservation,
                hasContractConflict,
                message,
                checkedAt
        );
    }

    @Override
    public void validateReservationAvailability(Long rentalUnitId, Long excludeReservationId) {
        LocalDate today = LocalDate.now();
        boolean hasActiveReservation = availabilityPolicyService.hasReservationConflict(
                rentalUnitId,
                LocalDateTime.now(),
                excludeReservationId,
                today,
                today
        );
        if (hasActiveReservation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La unidad tiene una reserva activa");
        }
    }

    @Override
    public void validateContractRentalUnitAvailability(
            Long rentalUnitId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeContractRentalUnitId
    ) {
        validateRequiredDateRange(startDate, endDate);
        boolean hasActiveReservation = availabilityPolicyService.hasReservationConflict(
                rentalUnitId,
                LocalDateTime.now(),
                null,
                startDate,
                endDate
        );
        if (hasActiveReservation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La unidad tiene una reserva activa");
        }

        RentalUnit rentalUnit = requireRentalUnit(rentalUnitId);
        AvailabilityPolicyService.ContractAvailabilityResult result =
                availabilityPolicyService.evaluateContractAvailability(
                        rentalUnit.getId(),
                        startDate,
                        endDate,
                        excludeContractRentalUnitId
                );

        if (!result.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    buildContractConflictMessage(result, startDate, endDate)
            );
        }
    }

    @Override
    public AvailabilitySummaryResponseDTO getAvailabilitySummary(
            Long warehouseId,
            Long sectorId,
            Long storageSpaceId
    ) {
        validateScope(warehouseId, sectorId, storageSpaceId);

        List<Long> rentalUnitIds = resolveRentalUnitIds(warehouseId, sectorId, storageSpaceId);
        if (rentalUnitIds.isEmpty()) {
            return new AvailabilitySummaryResponseDTO(
                    warehouseId,
                    sectorId,
                    storageSpaceId,
                    0,
                    0,
                    0,
                    0,
                    LocalDateTime.now()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<Long> contractedIds = availabilityPolicyService.findContractedRentalUnitIds(
                rentalUnitIds,
                today,
                today
        );
        List<Long> reservedIds = availabilityPolicyService.findReservedRentalUnitIds(
                rentalUnitIds,
                now,
                today,
                today
        );

        Set<Long> contractedSet = new HashSet<>(contractedIds);
        int contracted = contractedSet.size();
        int reserved = 0;
        for (Long reservedId : reservedIds) {
            if (!contractedSet.contains(reservedId)) {
                reserved++;
            }
        }

        int total = rentalUnitIds.size();
        int available = Math.max(0, total - reserved - contracted);

        return new AvailabilitySummaryResponseDTO(
                warehouseId,
                sectorId,
                storageSpaceId,
                total,
                available,
                reserved,
                contracted,
                LocalDateTime.now()
        );
    }

    private void validateScope(Long warehouseId, Long sectorId, Long storageSpaceId) {
        int provided = 0;
        if (warehouseId != null) {
            provided++;
        }
        if (sectorId != null) {
            provided++;
        }
        if (storageSpaceId != null) {
            provided++;
        }
        if (provided != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar exactamente uno: warehouseId, sectorId o storageSpaceId"
            );
        }
    }

    private List<Long> resolveRentalUnitIds(Long warehouseId, Long sectorId, Long storageSpaceId) {
        Map<Long, Long> unique = new LinkedHashMap<>();
        List<Long> results;

        if (warehouseId != null) {
            results = rentalUnitRepository.findIdsByWarehouseId(warehouseId);
            results.addAll(rentalUnitRepository.findIdsBySectorWarehouseId(warehouseId));
            results.addAll(rentalUnitRepository.findIdsByStorageSpaceSectorWarehouseId(warehouseId));
        } else if (sectorId != null) {
            results = rentalUnitRepository.findIdsBySectorId(sectorId);
            results.addAll(rentalUnitRepository.findIdsByStorageSpaceSectorId(sectorId));
        } else if (storageSpaceId != null) {
            results = rentalUnitRepository.findIdsByStorageSpaceId(storageSpaceId);
        } else {
            results = List.of();
        }

        for (Long id : results) {
            if (id != null) {
                unique.putIfAbsent(id, id);
            }
        }

        return List.copyOf(unique.values());
    }

    private RentalUnit requireRentalUnit(Long rentalUnitId) {
        return rentalUnitRepository.findByIdWithAssociations(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RENTAL_UNIT_NOT_FOUND));
    }

    private void validateDateFilters(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar startDate y endDate juntos para validar contratos"
            );
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de fechas no es valido");
        }
    }

    private void validateRequiredDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate y endDate son obligatorios para validar disponibilidad"
            );
        }
        validateDateFilters(startDate, endDate);
    }

    private String buildMessage(
            boolean hasActiveReservation,
            AvailabilityPolicyService.ContractAvailabilityResult contractAvailability,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (hasActiveReservation && contractAvailability.hasConflict()) {
            return "La unidad tiene una reserva activa y ademas existe un conflicto contractual en la jerarquia solicitada";
        }
        if (hasActiveReservation) {
            return "La unidad tiene una reserva activa";
        }
        if (contractAvailability.hasConflict()) {
            return buildContractConflictMessage(contractAvailability, startDate, endDate);
        }
        return "La unidad esta disponible";
    }

    private String buildContractConflictMessage(
            AvailabilityPolicyService.ContractAvailabilityResult contractAvailability,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (contractAvailability.hasActiveContract()) {
            return "La unidad ya tiene un contrato activo que se cruza entre " + startDate + " y " + endDate;
        }
        if (contractAvailability.hasRentedParent()) {
            return "La unidad no esta disponible porque una unidad padre ya esta rentada entre " + startDate + " y " + endDate;
        }
        if (contractAvailability.hasRentedChildren()) {
            return "La unidad no esta disponible porque una unidad hija ya esta rentada entre " + startDate + " y " + endDate;
        }
        return "La unidad esta disponible";
    }

    
}
