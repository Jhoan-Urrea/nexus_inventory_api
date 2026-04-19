package com.example.nexus.modules.sales.controller;

import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.sales.service.ContractDraftActivationService;
import com.example.nexus.modules.sales.service.PaymentApprovedClientEmailService;
import com.example.nexus.modules.sales.service.StripeWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final StripeWebhookService stripeWebhookService;
    private final ContractDraftActivationService contractDraftActivationService;
    private final PaymentApprovedClientEmailService paymentApprovedClientEmailService;
    private final ObjectMapper objectMapper;
    private final String endpointSecret;

    public StripeWebhookController(
            PaymentRepository paymentRepository,
            StripeWebhookService stripeWebhookService,
            ContractDraftActivationService contractDraftActivationService,
            PaymentApprovedClientEmailService paymentApprovedClientEmailService,
            ObjectMapper objectMapper,
            @Value("${stripe.webhook.secret}") String endpointSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.stripeWebhookService = stripeWebhookService;
        this.contractDraftActivationService = contractDraftActivationService;
        this.paymentApprovedClientEmailService = paymentApprovedClientEmailService;
        this.objectMapper = objectMapper;
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
        String paymentIntentId = resolvePaymentIntentId(event, payload);
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            log.warn("Stripe webhook {}: no se pudo resolver payment_intent id (payload API {})", event.getId(), event.getApiVersion());
            return;
        }

        Optional<Payment> payment = paymentRepository.findByPaymentExternalReference(paymentIntentId.trim());
        if (payment.isEmpty()) {
            log.warn(
                    "Stripe webhook: no hay pago local con payment_external_reference={} (revise que POST /api/sales/payments persista el pi antes de confirmar en el cliente)",
                    paymentIntentId
            );
            return;
        }

        updatePaymentStatus(payment.get(), status);

        try {
            stripeWebhookService.recordEvent(event, paymentIntentId.trim(), payload);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Webhook evento Stripe duplicado {}, idempotencia OK", event.getId());
        }
    }

    /**
     * Con API recientes el deserializador de Stripe a veces no devuelve {@link PaymentIntent}; el JSON del evento si trae {@code data.object.id}.
     */
    private String resolvePaymentIntentId(Event event, String payload) {
        Optional<Object> deserialized = event.getDataObjectDeserializer().getObject().map(obj -> (Object) obj);
        if (deserialized.isPresent() && deserialized.get() instanceof PaymentIntent pi) {
            return pi.getId();
        }
        if (deserialized.isPresent()) {
            log.warn(
                    "Stripe webhook {}: data.object es {} (no PaymentIntent). Se intenta extraer id del JSON.",
                    event.getId(),
                    deserialized.get().getClass().getName()
            );
        } else {
            log.warn("Stripe webhook {}: getObject() vacio; se intenta extraer id del JSON.", event.getId());
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode obj = root.path("data").path("object");
            if (obj.isMissingNode() || obj.isNull()) {
                return null;
            }
            if (!"payment_intent".equals(obj.path("object").asText())) {
                return null;
            }
            String id = obj.path("id").asText(null);
            return id != null && !id.isBlank() ? id : null;
        } catch (Exception e) {
            log.warn("Stripe webhook {}: error parseando payload: {}", event.getId(), e.getMessage());
            return null;
        }
    }

    private void updatePaymentStatus(Payment payment, PaymentStatus status) {
        PaymentStatus previous = payment.getPaymentStatus();
        if (previous != status) {
            payment.setPaymentStatus(status);
            paymentRepository.save(payment);
        }
        if (status == PaymentStatus.APPROVED) {
            contractDraftActivationService.activateIfDraftAndPaymentApproved(
                    payment.getContract().getId(),
                    PaymentStatus.APPROVED
            );
            if (previous != PaymentStatus.APPROVED) {
                paymentApprovedClientEmailService.sendPaymentApprovedEmail(payment.getId());
            }
        }
    }
}
