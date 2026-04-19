package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.RentalAvailabilityRequestDTO;
import com.example.nexus.modules.sales.dto.response.AvailabilitySummaryResponseDTO;
import com.example.nexus.modules.sales.dto.response.RentalAvailabilityResponseDTO;

import java.time.LocalDate;

public interface RentalAvailabilityService {

    boolean isRentalUnitAvailable(Long rentalUnitId, LocalDate startDate, LocalDate endDate);

    RentalAvailabilityResponseDTO validate(RentalAvailabilityRequestDTO request);

    void validateReservationAvailability(Long rentalUnitId, Long excludeReservationId);

    void validateContractRentalUnitAvailability(
            Long rentalUnitId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeContractRentalUnitId
    );

    AvailabilitySummaryResponseDTO getAvailabilitySummary(
            Long warehouseId,
            Long sectorId,
            Long storageSpaceId
    );

    java.util.List<com.example.nexus.modules.sales.dto.response.RentalUnitDTO> getCatalogWithAvailability(
            LocalDate startDate,
            LocalDate endDate
    );

    boolean validateBulk(com.example.nexus.modules.sales.dto.request.RentalBulkAvailabilityRequestDTO request);
}
