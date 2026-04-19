package com.example.nexus.modules.sales.service;

/** Notifica al cliente por correo cuando se crea una reserva (proceso de contratación en curso). */
public interface ReservationCreatedClientEmailService {

    void sendReservationCreatedEmail(Long reservationId);
}
