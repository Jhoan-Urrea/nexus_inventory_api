package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.response.ContractRentalUnitResponseDTO;
import com.example.nexus.modules.sales.mapper.ContractRentalUnitMapper;
import com.example.nexus.modules.sales.repository.ContractRentalUnitRepository;
import com.example.nexus.modules.sales.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractRentalUnitServiceImpl implements ContractRentalUnitService {

    private static final String MSG_CONTRACT_NOT_FOUND = "Contrato no encontrado";
    private static final String MSG_CONTRACT_RENTAL_UNIT_NOT_FOUND = "Registro de unidad rentada no encontrado";

    private final ContractRentalUnitRepository contractRentalUnitRepository;
    private final ContractRepository contractRepository;
    private final ContractRentalUnitMapper contractRentalUnitMapper;

    @Override
    public ContractRentalUnitResponseDTO findById(Long contractRentalUnitId) {
        return contractRentalUnitRepository.findByIdWithAssociations(contractRentalUnitId)
                .map(contractRentalUnitMapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        MSG_CONTRACT_RENTAL_UNIT_NOT_FOUND
                ));
    }

    @Override
    public List<ContractRentalUnitResponseDTO> findByContractId(Long contractId) {
        if (!contractRepository.existsById(contractId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND);
        }

        return contractRentalUnitRepository.findByContractIdOrderByStartDateAsc(contractId).stream()
                .map(contractRentalUnitMapper::toResponseDTO)
                .toList();
    }
}
