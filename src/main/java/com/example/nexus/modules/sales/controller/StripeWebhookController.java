package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.example.nexus.modules.sales.entity.StripeWebhookEvent;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.sales.repository.StripeWebhookEventRepository;
import com.example.nexus.modules.sales.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/payments/webhook/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentRepository paymentRepository;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final StripeWebhookService stripeWebhookService;
    private final String endpointSecret;

    public StripeWebhookController(
            PaymentRepository paymentRepository,
            StripeWebhookEventRepository stripeWebhookEventRepository,
            StripeWebhookService stripeWebhookService,
            @Value("${stripe.webhook.secret}") String endpointSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.stripeWebhookEventRepository = stripeWebhookEventRepository;
        this.stripeWebhookService = stripeWebhookService;
        this.endpointSecret = endpointSecret;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, endpointSecret);
        } catch (SignatureVerificationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if (event == null || event.getId() == null || event.getId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid event");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntent(event, PaymentStatus.APPROVED, payload);
            case "payment_intent.payment_failed" -> handlePaymentIntent(event, PaymentStatus.FAILED, payload);
            default -> {
                return ResponseEntity.ok("Event ignored");
            }
        }

        return ResponseEntity.ok("Event processed");
    }

    private void handlePaymentIntent(Event event, PaymentStatus status, String payload) {
        Optional<Object> deserialized = event.getDataObjectDeserializer().getObject().map(obj -> (Object) obj);
        if (deserialized.isEmpty()) {
            return;
        }

        Object stripeObject = deserialized.get();
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }

        Optional<Payment> payment = paymentRepository.findByPaymentExternalReference(paymentIntentId);
        if (payment.isEmpty()) {
            log.warn("Stripe webhook received for unknown payment intent: {}", paymentIntentId);
            return;
        }

        if (stripeWebhookEventRepository.existsByEventId(event.getId())) {
            return;
        }

        try {
            stripeWebhookService.recordEvent(event, paymentIntentId, payload);
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        updatePaymentStatus(payment.get(), status);
    }

    private void updatePaymentStatus(Payment payment, PaymentStatus status) {
        if (payment.getPaymentStatus() != status) {
            payment.setPaymentStatus(status);
            paymentRepository.save(payment);
        }
    }
}
