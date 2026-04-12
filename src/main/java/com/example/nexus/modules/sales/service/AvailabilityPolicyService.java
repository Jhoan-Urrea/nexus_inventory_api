package com.example.nexus.modules.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AvailabilityPolicyService {

    boolean hasReservationConflict(
            Long rentalUnitId,
            LocalDateTime referenceTime,
            Long excludeReservationId,
            LocalDate startDate,
            LocalDate endDate
    );

    ContractAvailabilityResult evaluateContractAvailability(
            Long rentalUnitId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeContractRentalUnitId
    );

    List<Long> findReservedRentalUnitIds(
            Collection<Long> rentalUnitIds,
            LocalDateTime referenceTime,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Long> findContractedRentalUnitIds(
            Collection<Long> rentalUnitIds,
            LocalDate startDate,
            LocalDate endDate
    );

    record ContractAvailabilityResult(
            boolean hasActiveContract,
            boolean hasRentedParent,
            boolean hasRentedChildren
    ) {
        public static ContractAvailabilityResult available() {
            return new ContractAvailabilityResult(false, false, false);
        }

        public boolean hasConflict() {
            return hasActiveContract || hasRentedParent || hasRentedChildren;
        }

        public boolean isAvailable() {
            return !hasConflict();
        }
    }
}
