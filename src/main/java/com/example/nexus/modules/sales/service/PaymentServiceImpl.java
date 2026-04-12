package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO;
import com.example.nexus.modules.sales.dto.response.PaymentResponseDTO;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.example.nexus.modules.sales.mapper.PaymentMapper;
import com.example.nexus.modules.sales.payment.gateway.PaymentGateway;
import com.example.nexus.modules.sales.payment.gateway.PaymentGatewayRequest;
import com.example.nexus.modules.sales.payment.gateway.PaymentGatewayResult;
import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.sales.security.OwnershipValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final String MSG_CONTRACT_NOT_FOUND = "Contrato no encontrado";

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGateway paymentGateway;
    private final OwnershipValidationService ownershipValidationService;
    private final ContractStateMachineService contractStateMachineService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDTO registerPayment(CreatePaymentRequestDTO request) {
        ownershipValidationService.validateContractOwnership(request.contractId(), currentAuthentication());
        Contract contract = requireContract(request.contractId());
        ensureNoDuplicatePayment(request);

        Payment payment = paymentMapper.toEntity(request, contract);

        PaymentGatewayResult gatewayResult = paymentGateway.createPaymentIntent(
                new PaymentGatewayRequest(
                        contract.getId(),
                        payment.getAmount(),
                        payment.getPaymentMethod(),
                        payment.getPaymentReference(),
                        payment.getPaymentExternalReference(),
                        payment.getPaymentStatus()
                )
        );

        if (gatewayResult != null) {
            if (gatewayResult.status() != null) {
                payment.setPaymentStatus(gatewayResult.status());
            }
            if (gatewayResult.paymentReference() != null && !gatewayResult.paymentReference().isBlank()) {
                payment.setPaymentReference(gatewayResult.paymentReference());
            }
            if (gatewayResult.paymentExternalReference() != null && !gatewayResult.paymentExternalReference().isBlank()) {
                payment.setPaymentExternalReference(gatewayResult.paymentExternalReference());
            }
        }
        contract.addPayment(payment);
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago duplicado", ex);
        }

        activateContractIfApproved(savedPayment, contract);
        return paymentMapper.toResponseDTO(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByContract(Long contractId) {
        ownershipValidationService.validateContractOwnership(contractId, currentAuthentication());
        requireContract(contractId);

        return paymentRepository.findByContractIdOrderByPaymentDateDesc(contractId).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

    private Contract requireContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
    }

    private void ensureNoDuplicatePayment(CreatePaymentRequestDTO request) {
        if (request.paymentReference() != null && !request.paymentReference().isBlank()) {
            if (paymentRepository.existsByPaymentReference(request.paymentReference().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Referencia de pago duplicada");
            }
        }

        if (request.paymentExternalReference() != null && !request.paymentExternalReference().isBlank()) {
            if (paymentRepository.existsByPaymentExternalReference(request.paymentExternalReference().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Referencia externa de pago duplicada");
            }
        }
    }

    private void activateContractIfApproved(Payment payment, Contract contract) {
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) {
            return;
        }

        ContractStatus currentStatus = ContractStatus.fromCode(contract.getStatus());
        if (currentStatus == null || currentStatus == ContractStatus.DRAFT) {
            contractStateMachineService.transitionStatus(contract, ContractStatus.ACTIVE);
            contractRepository.save(contract);
        }
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
