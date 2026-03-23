package com.example.nexus.testsupport;

import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;

import java.util.UUID;

/**
 * Cliente persistible en tests: cumple NOT NULL y unicidad de email/documentNumber.
 */
public final class ClientTestFixtures {

    private ClientTestFixtures() {
    }

    public static Client newClient(String displayName) {
        String uniq = UUID.randomUUID().toString().substring(0, 8);
        String slug = displayName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (slug.isBlank()) {
            slug = "client";
        }
        return Client.builder()
                .name(displayName)
                .email(slug + "-" + uniq + "@test.nexus.local")
                .phone("3000000000")
                .documentType("NIT")
                .documentNumber("NIT-" + uniq)
                .businessName(displayName + " Business")
                .address("Calle Test 123")
                .status(ClientStatus.ACTIVE)
                .build();
    }
}
