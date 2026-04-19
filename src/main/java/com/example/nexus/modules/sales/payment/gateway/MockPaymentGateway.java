package com.example.nexus.modules.sales.payment.gateway;

import com.example.nexus.modules.sales.entity.PaymentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("test")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult createPaymentIntent(PaymentGatewayRequest request) {
        PaymentStatus status = request.requestedStatus() != null
                ? request.requestedStatus()
                : PaymentStatus.PENDING;

        String externalReference = request.paymentExternalReference();
        if (externalReference == null || externalReference.isBlank()) {
            externalReference = UUID.randomUUID().toString();
        }

        return new PaymentGatewayResult(
                status,
                request.paymentReference(),
                externalReference,
                null
        );
    }

    @Override
    public PaymentGatewayResult retrievePaymentIntent(String paymentExternalReference) {
        return new PaymentGatewayResult(PaymentStatus.PENDING, null, paymentExternalReference, null);
    }

    @Override
    public PaymentGatewayResult confirmPayment(String paymentExternalReference) {
        return new PaymentGatewayResult(PaymentStatus.APPROVED, null, paymentExternalReference, null);
    }

    @Override
    public PaymentGatewayResult cancelPayment(String paymentExternalReference) {
        return new PaymentGatewayResult(PaymentStatus.FAILED, null, paymentExternalReference, null);
    }
}
