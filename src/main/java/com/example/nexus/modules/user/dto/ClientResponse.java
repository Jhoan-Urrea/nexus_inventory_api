package com.example.nexus.modules.user.dto;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        String documentType,
        String documentNumber,
        String businessName,
        String address,
        String status
) {
}
