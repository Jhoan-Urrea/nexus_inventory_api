package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.example.nexus.modules.sales.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractDraftActivationService {

    private final ContractRepository contractRepository;
    private final ContractStateMachineService contractStateMachineService;

    @Transactional
    public void activateIfDraftAndPaymentApproved(Long contractId, PaymentStatus paymentStatus) {
        if (contractId == null || paymentStatus != PaymentStatus.APPROVED) {
            return;
        }
        contractRepository.findById(contractId).ifPresent(contract -> {
            ContractStatus current = ContractStatus.fromCode(contract.getStatus());
            if (current == ContractStatus.DRAFT) {
                contractStateMachineService.transitionStatus(contract, ContractStatus.ACTIVE);
                contractRepository.save(contract);
            }
        });
    }
}
