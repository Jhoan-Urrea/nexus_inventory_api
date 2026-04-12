package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO;
import com.example.nexus.modules.sales.dto.response.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    PaymentResponseDTO registerPayment(CreatePaymentRequestDTO request);

    List<PaymentResponseDTO> getPaymentsByContract(Long contractId);
}
