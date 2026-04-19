package com.example.nexus.modules.sales.service;

/** Correo al cliente cuando su pago queda en estado APPROVED (confirmación y datos del contrato). */
public interface PaymentApprovedClientEmailService {

    void sendPaymentApprovedEmail(Long paymentId);
}
