package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ClientResponse(
        @Schema(description = "ID del cliente", example = "1")
        Long id,

        @Schema(description = "Nombre", example = "Acme Corp")
        String name,

        @Schema(description = "Correo electrónico", example = "contact@acme.com")
        String email,

        @Schema(description = "Teléfono")
        String phone,

        @Schema(description = "Tipo de documento", example = "NIT")
        String documentType,

        @Schema(description = "Número de documento", example = "900123456-1")
        String documentNumber,

        @Schema(description = "Razón social", example = "Acme Corp S.A.S.")
        String businessName,

        @Schema(description = "Dirección")
        String address,

        @Schema(description = "Estado", example = "ACTIVE")
        String status,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización")
        LocalDateTime updatedAt
) {
}
