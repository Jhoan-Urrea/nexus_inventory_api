package com.example.nexus.modules.sales.payment.gateway;

import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.PostConstruct;

@Service
@Profile("!test")
public class StripePaymentGateway implements PaymentGateway {

    /** Monedas sin decimales en Stripe (monto entero en unidad principal). */
    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "bif", "clp", "djf", "gnf", "jpy", "krw", "mga", "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf"
    );

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
        String effectiveCurrency = resolveEffectiveCurrency(request.chargeCurrency());
        long amountInMinor = toMinorUnits(request.amount(), effectiveCurrency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinor)
                .setCurrency(effectiveCurrency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("contractId", String.valueOf(request.contractId()))
                .putMetadata("chargeCurrency", effectiveCurrency)
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return new PaymentGatewayResult(
                    mapStatus(intent.getStatus()),
                    request.paymentReference(),
                    intent.getId(),
                    intent.getClientSecret()
            );
        } catch (StripeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Stripe payment intent creation failed";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg, ex);
        }
    }

    @Override
    public PaymentGatewayResult retrievePaymentIntent(String paymentExternalReference) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentExternalReference);
            return new PaymentGatewayResult(
                    mapStatus(intent.getStatus()),
                    null,
                    intent.getId(),
                    intent.getClientSecret()
            );
        } catch (StripeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    ex.getMessage() != null ? ex.getMessage() : "Stripe payment intent retrieval failed",
                    ex
            );
        }
    }

    private String resolveEffectiveCurrency(String fromContract) {
        if (fromContract != null && !fromContract.isBlank()) {
            return fromContract.trim().toLowerCase(Locale.ROOT);
        }
        return currency != null ? currency.trim().toLowerCase(Locale.ROOT) : "usd";
    }

    @Override
    public PaymentGatewayResult confirmPayment(String paymentExternalReference) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentExternalReference);
            PaymentIntent confirmed = intent.confirm();
            return new PaymentGatewayResult(
                    mapStatus(confirmed.getStatus()),
                    null,
                    confirmed.getId(),
                    null
            );
        } catch (StripeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    ex.getMessage() != null ? ex.getMessage() : "Stripe payment confirmation failed",
                    ex
            );
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
                    canceled.getId(),
                    null
            );
        } catch (StripeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    ex.getMessage() != null ? ex.getMessage() : "Stripe payment cancellation failed",
                    ex
            );
        }
    }

    private long toMinorUnits(BigDecimal amount, String currencyLower) {
        if (amount == null) {
            return 0L;
        }
        BigDecimal factor = ZERO_DECIMAL_CURRENCIES.contains(currencyLower)
                ? BigDecimal.ONE
                : BigDecimal.valueOf(100);
        return amount.multiply(factor)
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
