package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.entity.ReservationStatus;
import com.example.nexus.modules.sales.repository.ContractRentalUnitRepository;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityPolicyServiceImpl implements AvailabilityPolicyService {

    static final int ACTIVE_RESERVATION_STATUS = ReservationStatus.PENDING.getCode();
    private static final String MSG_RENTAL_UNIT_NOT_FOUND = "Unidad de renta no encontrada";

    private final RentalUnitRepository rentalUnitRepository;
    private final ReservationRepository reservationRepository;
    private final ContractRentalUnitRepository contractRentalUnitRepository;

    @Override
    public boolean hasReservationConflict(
            Long rentalUnitId,
            LocalDateTime referenceTime,
            Long excludeReservationId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : referenceTime.toLocalDate();
        LocalDate effectiveEnd = endDate != null ? endDate : referenceTime.toLocalDate();

        RentalUnit rentalUnit = requireRentalUnit(rentalUnitId);

        if (reservationRepository.existsActiveReservation(
                rentalUnit.getId(),
                referenceTime,
                excludeReservationId,
                ACTIVE_RESERVATION_STATUS,
                effectiveStart,
                effectiveEnd
        )) {
            return true;
        }

        Set<Long> relatedIds = new LinkedHashSet<>();
        relatedIds.addAll(resolveParentRentalUnitIds(rentalUnit));
        relatedIds.addAll(resolveChildRentalUnitIds(rentalUnit));

        for (Long relatedId : relatedIds) {
            if (reservationRepository.existsActiveReservation(
                    relatedId,
                    referenceTime,
                    excludeReservationId,
                    ACTIVE_RESERVATION_STATUS,
                    effectiveStart,
                    effectiveEnd
            )) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ContractAvailabilityResult evaluateContractAvailability(
            Long rentalUnitId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeContractRentalUnitId
    ) {
        RentalUnit rentalUnit = requireRentalUnit(rentalUnitId);

        boolean hasActiveContract = contractRentalUnitRepository.existsOverlappingRentalUnit(
                rentalUnit.getId(),
                startDate,
                endDate,
                excludeContractRentalUnitId
        );

        Set<Long> parentIds = resolveParentRentalUnitIds(rentalUnit);
        Set<Long> childIds = resolveChildRentalUnitIds(rentalUnit);

        boolean hasRentedParent = !parentIds.isEmpty()
                && contractRentalUnitRepository.existsOverlappingRentalUnits(
                parentIds,
                startDate,
                endDate,
                excludeContractRentalUnitId
        );
        boolean hasRentedChildren = !childIds.isEmpty()
                && contractRentalUnitRepository.existsOverlappingRentalUnits(
                childIds,
                startDate,
                endDate,
                excludeContractRentalUnitId
        );

        return new ContractAvailabilityResult(hasActiveContract, hasRentedParent, hasRentedChildren);
    }

    @Override
    public List<Long> findReservedRentalUnitIds(
            Collection<Long> rentalUnitIds,
            LocalDateTime referenceTime,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (rentalUnitIds == null || rentalUnitIds.isEmpty()) {
            return List.of();
        }
        return reservationRepository.findActiveReservationRentalUnitIds(
                rentalUnitIds,
                referenceTime,
                ACTIVE_RESERVATION_STATUS,
                startDate,
                endDate
        );
    }

    @Override
    public List<Long> findContractedRentalUnitIds(
            Collection<Long> rentalUnitIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (rentalUnitIds == null || rentalUnitIds.isEmpty()) {
            return List.of();
        }
        return contractRentalUnitRepository.findContractedRentalUnitIds(
                rentalUnitIds,
                startDate,
                endDate
        );
    }

    private RentalUnit requireRentalUnit(Long rentalUnitId) {
        return rentalUnitRepository.findByIdWithAssociations(rentalUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RENTAL_UNIT_NOT_FOUND));
    }

    private Collection<Long> resolveWarehouseRentalUnitIds(Long warehouseId) {
        return rentalUnitRepository.findByWarehouseId(warehouseId).stream()
                .map(RentalUnit::getId)
                .toList();
    }

    private Collection<Long> resolveSectorRentalUnitIds(Long sectorId) {
        return rentalUnitRepository.findBySectorId(sectorId).stream()
                .map(RentalUnit::getId)
                .toList();
    }

    private Collection<Long> resolveStorageSpaceRentalUnitIdsBySector(Long sectorId) {
        return rentalUnitRepository.findByStorageSpaceSectorId(sectorId).stream()
                .map(RentalUnit::getId)
                .toList();
    }

    private Collection<Long> resolveStorageSpaceRentalUnitIdsByWarehouse(Long warehouseId) {
        return rentalUnitRepository.findByStorageSpaceSectorWarehouseId(warehouseId).stream()
                .map(RentalUnit::getId)
                .toList();
    }

    private Set<Long> resolveParentRentalUnitIds(RentalUnit rentalUnit) {
        Set<Long> parentIds = new LinkedHashSet<>();

        if (rentalUnit.getSector() != null) {
            parentIds.addAll(resolveWarehouseRentalUnitIds(rentalUnit.getSector().getWarehouse().getId()));
        }

        if (rentalUnit.getStorageSpace() != null) {
            parentIds.addAll(resolveSectorRentalUnitIds(rentalUnit.getStorageSpace().getSector().getId()));
            parentIds.addAll(resolveWarehouseRentalUnitIds(
                    rentalUnit.getStorageSpace().getSector().getWarehouse().getId()
            ));
        }

        parentIds.remove(rentalUnit.getId());
        return parentIds;
    }

    private Set<Long> resolveChildRentalUnitIds(RentalUnit rentalUnit) {
        Set<Long> childIds = new LinkedHashSet<>();

        if (rentalUnit.getWarehouse() != null) {
            childIds.addAll(resolveSectorRentalUnitIds(rentalUnit.getWarehouse().getId()));
            childIds.addAll(resolveStorageSpaceRentalUnitIdsByWarehouse(rentalUnit.getWarehouse().getId()));
        }

        if (rentalUnit.getSector() != null) {
            childIds.addAll(resolveStorageSpaceRentalUnitIdsBySector(rentalUnit.getSector().getId()));
        }

        childIds.remove(rentalUnit.getId());
        return childIds;
    }
}
