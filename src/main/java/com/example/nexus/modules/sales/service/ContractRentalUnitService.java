package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.response.ContractRentalUnitResponseDTO;

import java.util.List;

public interface ContractRentalUnitService {

    ContractRentalUnitResponseDTO findById(Long contractRentalUnitId);

    List<ContractRentalUnitResponseDTO> findByContractId(Long contractId);
}
