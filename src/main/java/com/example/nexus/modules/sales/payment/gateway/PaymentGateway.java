package com.example.nexus.modules.sales.payment.gateway;

public interface PaymentGateway {

    PaymentGatewayResult createPaymentIntent(PaymentGatewayRequest request);

    PaymentGatewayResult confirmPayment(String paymentExternalReference);

    PaymentGatewayResult cancelPayment(String paymentExternalReference);
}
