package com.example.nexus.modules.sales.mapper;

import com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO;
import com.example.nexus.modules.sales.dto.response.PaymentResponseDTO;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(CreatePaymentRequestDTO dto, Contract contract) {
        return Payment.builder()
                .contract(contract)
                .amount(dto.amount())
                .paymentStatus(PaymentStatus.fromString(dto.paymentStatus()))
                .paymentMethod(dto.paymentMethod())
                .paymentReference(dto.paymentReference())
                .paymentExternalReference(dto.paymentExternalReference())
                .build();
    }

    public PaymentResponseDTO toResponseDTO(Payment entity) {
        return toResponseDTO(entity, null);
    }

    public PaymentResponseDTO toResponseDTO(Payment entity, String stripeClientSecret) {
        return new PaymentResponseDTO(
                entity.getId(),
                entity.getContract() != null ? entity.getContract().getId() : null,
                entity.getAmount(),
                entity.getPaymentDate(),
                entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : null,
                entity.getPaymentMethod(),
                entity.getPaymentReference(),
                entity.getPaymentExternalReference(),
                stripeClientSecret
        );
    }
}
