package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContractStateMachineServiceImpl implements ContractStateMachineService {

    @Override
    public Contract transitionStatus(Contract contract, ContractStatus newStatus) {
        if (contract == null || newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de contrato invalido");
        }

        ContractStatus current = ContractStatus.fromCode(contract.getStatus());
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado actual de contrato invalido");
        }

        if (!isAllowedTransition(current, newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transicion no permitida de " + current + " a " + newStatus
            );
        }

        contract.setStatus(newStatus.getCode());
        return contract;
    }

    private boolean isAllowedTransition(ContractStatus current, ContractStatus target) {
        return (current == ContractStatus.DRAFT && target == ContractStatus.ACTIVE)
                || (current == ContractStatus.ACTIVE && target == ContractStatus.COMPLETED)
                || (current == ContractStatus.ACTIVE && target == ContractStatus.CANCELLED);
    }
}
