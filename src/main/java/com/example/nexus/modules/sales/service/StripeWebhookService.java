package com.example.nexus.modules.sales.service;

import com.stripe.model.Event;

public interface StripeWebhookService {

    void recordEvent(Event stripeEvent, String paymentIntentId, String payload);
}
