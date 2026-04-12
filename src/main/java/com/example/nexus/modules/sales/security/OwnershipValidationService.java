package com.example.nexus.modules.sales.security;

import org.springframework.security.core.Authentication;

public interface OwnershipValidationService {

    void validateReservationOwnership(Long reservationId, Authentication authenticatedUser);

    void validateContractOwnership(Long contractId, Authentication authenticatedUser);

    void validatePaymentOwnership(Long paymentId, Authentication authenticatedUser);

    boolean isAdmin(Authentication authenticatedUser);

    Long requireClientId(Authentication authenticatedUser);
}
