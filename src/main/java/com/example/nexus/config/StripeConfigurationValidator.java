package com.example.nexus.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("prod")
@Component
public class StripeConfigurationValidator {

    private final String secretKey;
    private final String webhookSecret;

    public StripeConfigurationValidator(
            @Value("${stripe.secret-key:}") String secretKey,
            @Value("${stripe.webhook.secret:}") String webhookSecret
    ) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isBlank() || webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe configuration missing");
        }
    }
}
