package com.example.nexus.modules.sales.payment.gateway;

public interface PaymentGateway {

    PaymentGatewayResult createPaymentIntent(PaymentGatewayRequest request);

    /** Estado actual del intent y {@code client_secret} si aplica (p. ej. reanudar pago pendiente). */
    PaymentGatewayResult retrievePaymentIntent(String paymentExternalReference);

    PaymentGatewayResult confirmPayment(String paymentExternalReference);

    PaymentGatewayResult cancelPayment(String paymentExternalReference);
}
