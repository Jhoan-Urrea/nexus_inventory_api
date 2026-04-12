package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.StripeWebhookEvent;
import com.example.nexus.modules.sales.repository.StripeWebhookEventRepository;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private final StripeWebhookEventRepository stripeWebhookEventRepository;

    @Override
    public void recordEvent(Event stripeEvent, String paymentIntentId, String payload) {
        StripeWebhookEvent event = StripeWebhookEvent.builder()
                .eventId(stripeEvent.getId())
                .eventType(stripeEvent.getType())
                .paymentIntent(paymentIntentId)
                .payload(payload)
                .processed(true)
                .build();

        stripeWebhookEventRepository.save(event);
    }
}
