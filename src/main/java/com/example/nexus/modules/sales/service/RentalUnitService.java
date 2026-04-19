package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.UpdateRentalUnitPricingRequestDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitPricingDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;

import java.util.List;

public interface RentalUnitService {

    RentalUnit create(RentalUnit rentalUnit);

    List<RentalUnit> findAll();

    RentalUnit findById(Long id);

    RentalUnit update(Long id, RentalUnit rentalUnit);

    List<RentalUnitPricingDTO> findPricingCatalog(Boolean readyOnly, Boolean activeOnly);

    RentalUnitPricingDTO updatePricing(Long id, UpdateRentalUnitPricingRequestDTO request, String actorEmail);

    void delete(Long id);
}
