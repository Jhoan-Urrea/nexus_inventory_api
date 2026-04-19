package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreateContractRequest;
import com.example.nexus.modules.sales.dto.response.ContractResponseDTO;

import java.util.List;

public interface ContractService {

    ContractResponseDTO createContract(CreateContractRequest request);

    ContractResponseDTO findById(Long contractId);

    List<ContractResponseDTO> findAll();

    /** Contratos en estado ACTIVE del cliente autenticado (p. ej. Mis bodegas). */
    List<ContractResponseDTO> findMyActiveContracts();

    ContractResponseDTO completeContract(Long contractId);

    ContractResponseDTO cancelContract(Long contractId);
}
