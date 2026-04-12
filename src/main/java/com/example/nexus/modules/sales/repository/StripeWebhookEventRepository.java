package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}
