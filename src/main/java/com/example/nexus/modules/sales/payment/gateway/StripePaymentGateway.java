package com.example.nexus.modules.sales.payment.gateway;

import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import jakarta.annotation.PostConstruct;

@Service
public class StripePaymentGateway implements PaymentGateway {

    private final String apiKey;
    private final String currency;

    public StripePaymentGateway(
            @Value("${stripe.secret-key}") String apiKey,
            @Value("${stripe.currency:usd}") String currency
    ) {
        this.apiKey = apiKey;
        this.currency = currency;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    @Override
    public PaymentGatewayResult createPaymentIntent(PaymentGatewayRequest request) {
        long amountInMinor = toMinorUnits(request.amount());

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinor)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("contractId", String.valueOf(request.contractId()))
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return new PaymentGatewayResult(
                    mapStatus(intent.getStatus()),
                    request.paymentReference(),
                    intent.getId()
            );
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe payment intent creation failed", ex);
        }
    }

    @Override
    public PaymentGatewayResult confirmPayment(String paymentExternalReference) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentExternalReference);
            PaymentIntent confirmed = intent.confirm();
            return new PaymentGatewayResult(
                    mapStatus(confirmed.getStatus()),
                    null,
                    confirmed.getId()
            );
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe payment confirmation failed", ex);
        }
    }

    @Override
    public PaymentGatewayResult cancelPayment(String paymentExternalReference) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentExternalReference);
            PaymentIntent canceled = intent.cancel();
            return new PaymentGatewayResult(
                    mapStatus(canceled.getStatus()),
                    null,
                    canceled.getId()
            );
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe payment cancellation failed", ex);
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private PaymentStatus mapStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        return switch (status) {
            case "succeeded" -> PaymentStatus.APPROVED;
            case "canceled" -> PaymentStatus.FAILED;
            case "requires_payment_method", "requires_confirmation", "requires_action", "processing" ->
                    PaymentStatus.PENDING;
            default -> PaymentStatus.PENDING;
        };
    }
}
