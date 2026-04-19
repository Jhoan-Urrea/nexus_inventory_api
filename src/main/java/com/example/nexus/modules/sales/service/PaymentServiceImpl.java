package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO;
import com.example.nexus.modules.sales.dto.response.PaymentResponseDTO;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.RentalUnit;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final String MSG_CONTRACT_NOT_FOUND = "Contrato no encontrado";
    private static final String STRIPE_METHOD = "STRIPE";

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGateway paymentGateway;
    private final OwnershipValidationService ownershipValidationService;
    private final ContractDraftActivationService contractDraftActivationService;
    private final PaymentApprovedClientEmailService paymentApprovedClientEmailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDTO registerPayment(CreatePaymentRequestDTO request) {
        ownershipValidationService.validateContractOwnership(request.contractId(), currentAuthentication());
        Contract contract = requireContractWithAssociations(request.contractId());
        validatePaymentAmountAgainstContract(request.amount(), contract);
        validateContractAcceptsNewPayment(contract);

        Optional<PaymentResponseDTO> reused = tryReusePendingStripePayment(contract, request);
        if (reused.isPresent()) {
            return reused.get();
        }

        ensureNoDuplicatePayment(request);

        Payment payment = paymentMapper.toEntity(request, contract);
        String chargeCurrency = resolveStripeChargeCurrency(contract);

        PaymentGatewayResult gatewayResult = paymentGateway.createPaymentIntent(
                new PaymentGatewayRequest(
                        contract.getId(),
                        payment.getAmount(),
                        payment.getPaymentMethod(),
                        payment.getPaymentReference(),
                        payment.getPaymentExternalReference(),
                        payment.getPaymentStatus(),
                        chargeCurrency
                )
        );

        String stripeClientSecret = null;
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
            stripeClientSecret = gatewayResult.clientSecret();
        }
        contract.addPayment(payment);
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago duplicado", ex);
        }

        contractDraftActivationService.activateIfDraftAndPaymentApproved(
                contract.getId(),
                savedPayment.getPaymentStatus()
        );
        if (savedPayment.getPaymentStatus() == PaymentStatus.APPROVED) {
            paymentApprovedClientEmailService.sendPaymentApprovedEmail(savedPayment.getId());
        }
        return paymentMapper.toResponseDTO(savedPayment, stripeClientSecret);
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

    private Contract requireContractWithAssociations(Long contractId) {
        return contractRepository.findByIdWithAssociations(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
    }

    private String resolveStripeChargeCurrency(Contract contract) {
        List<ContractRentalUnit> lines = contract.getRentalUnits();
        if (lines == null || lines.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El contrato no tiene lineas; no se puede determinar la moneda para Stripe"
            );
        }
        String currency = null;
        for (ContractRentalUnit line : lines) {
            RentalUnit ru = line.getRentalUnit();
            if (ru == null || ru.getCurrency() == null || ru.getCurrency().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Una linea del contrato referencia una unidad sin moneda configurada"
                );
            }
            String c = ru.getCurrency().trim().toLowerCase(Locale.ROOT);
            if (currency == null) {
                currency = c;
            } else if (!currency.equals(c)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El contrato mezcla monedas distintas; unifique precios o divida el contrato"
                );
            }
        }
        return currency;
    }

    /**
     * Evita multiples {@link PaymentIntent} por el mismo contrato: reutiliza el ultimo pago Stripe
     * {@link PaymentStatus#PENDING} con intent creado y sincroniza estado con Stripe. Si el intent
     * quedo cancelado, marca el registro como fallido y permite crear uno nuevo.
     */
    private Optional<PaymentResponseDTO> tryReusePendingStripePayment(Contract contract, CreatePaymentRequestDTO request) {
        if (!isStripeMethod(request.paymentMethod())) {
            return Optional.empty();
        }
        Optional<Payment> candidate = paymentRepository
                .findFirstByContract_IdAndPaymentStatusOrderByPaymentDateDesc(contract.getId(), PaymentStatus.PENDING)
                .filter(p -> p.getPaymentExternalReference() != null && !p.getPaymentExternalReference().isBlank())
                .filter(p -> isStripeMethod(p.getPaymentMethod()))
                .filter(p -> p.getAmount() != null && p.getAmount().compareTo(request.amount()) == 0);

        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        Payment pending = candidate.get();
        PaymentStatus statusBeforeSync = pending.getPaymentStatus();
        PaymentGatewayResult refreshed = paymentGateway.retrievePaymentIntent(pending.getPaymentExternalReference());

        if (refreshed.status() == PaymentStatus.FAILED) {
            pending.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(pending);
            return Optional.empty();
        }

        mergeGatewayIntoPayment(pending, refreshed);
        Payment saved = paymentRepository.save(pending);
        contractDraftActivationService.activateIfDraftAndPaymentApproved(
                contract.getId(),
                saved.getPaymentStatus()
        );
        if (saved.getPaymentStatus() == PaymentStatus.APPROVED && statusBeforeSync != PaymentStatus.APPROVED) {
            paymentApprovedClientEmailService.sendPaymentApprovedEmail(saved.getId());
        }
        return Optional.of(paymentMapper.toResponseDTO(saved, refreshed.clientSecret()));
    }

    private static boolean isStripeMethod(String paymentMethod) {
        return paymentMethod != null && STRIPE_METHOD.equalsIgnoreCase(paymentMethod.trim());
    }

    private static void mergeGatewayIntoPayment(Payment payment, PaymentGatewayResult result) {
        if (result.status() != null) {
            payment.setPaymentStatus(result.status());
        }
        if (result.paymentExternalReference() != null && !result.paymentExternalReference().isBlank()) {
            payment.setPaymentExternalReference(result.paymentExternalReference());
        }
        if (result.paymentReference() != null && !result.paymentReference().isBlank()) {
            payment.setPaymentReference(result.paymentReference());
        }
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

    private void validateContractAcceptsNewPayment(Contract contract) {
        ContractStatus contractStatus = ContractStatus.fromCode(contract.getStatus());
        if (contractStatus == ContractStatus.CANCELLED || contractStatus == ContractStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El contrato no admite nuevos pagos en su estado actual"
            );
        }
        BigDecimal totalPaidApproved = paymentRepository.sumAmountByContractIdAndPaymentStatus(
                contract.getId(),
                PaymentStatus.APPROVED
        );
        if (totalPaidApproved == null) {
            totalPaidApproved = BigDecimal.ZERO;
        }
        if (contract.getTotalAmount() != null
                && totalPaidApproved.compareTo(contract.getTotalAmount()) >= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El monto total del contrato ya fue cubierto por pagos aprobados"
            );
        }
    }

    private void validatePaymentAmountAgainstContract(BigDecimal paymentAmount, Contract contract) {
        if (contract.getTotalAmount() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El contrato no tiene monto total configurado");
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del pago debe ser mayor a cero");
        }
        if (paymentAmount.compareTo(contract.getTotalAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El monto del pago debe coincidir con el total del contrato"
            );
        }
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
