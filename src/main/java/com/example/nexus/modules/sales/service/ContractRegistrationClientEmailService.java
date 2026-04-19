package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.Reservation;

/** Notifica al cliente por correo cuando se registra un contrato a su nombre (incluye indicaciones de pago). */
public interface ContractRegistrationClientEmailService {

    /**
     * @param fromReservation reserva de origen cuando el contrato surge de una reserva; {@code null} si es contrato directo
     */
    void sendContractRegisteredToClient(Contract contract, Reservation fromReservation);
}
