package com.example.nexus.modules.sales.security;

import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OwnershipValidationServiceImpl implements OwnershipValidationService {

    private static final String MSG_RESERVATION_NOT_FOUND = "Reserva no encontrada";
    private static final String MSG_CONTRACT_NOT_FOUND = "Contrato no encontrado";
    private static final String MSG_PAYMENT_NOT_FOUND = "Pago no encontrado";
    private static final String MSG_ACCESS_DENIED = "Access denied";

    private final AppUserRepository appUserRepository;
    private final ReservationRepository reservationRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public void validateReservationOwnership(Long reservationId, Authentication authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        Long clientId = requireClientId(authenticatedUser);
        Long reservationClientId = reservationRepository.findClientIdByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_RESERVATION_NOT_FOUND));
        if (!reservationClientId.equals(clientId)) {
            throw new AccessDeniedException(MSG_ACCESS_DENIED);
        }
    }

    @Override
    public void validateContractOwnership(Long contractId, Authentication authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        Long clientId = requireClientId(authenticatedUser);
        Long contractClientId = contractRepository.findClientIdByContractId(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_CONTRACT_NOT_FOUND));
        if (!contractClientId.equals(clientId)) {
            throw new AccessDeniedException(MSG_ACCESS_DENIED);
        }
    }

    @Override
    public void validatePaymentOwnership(Long paymentId, Authentication authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        Long clientId = requireClientId(authenticatedUser);
        Long paymentClientId = paymentRepository.findClientIdByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_PAYMENT_NOT_FOUND));
        if (!paymentClientId.equals(clientId)) {
            throw new AccessDeniedException(MSG_ACCESS_DENIED);
        }
    }

    @Override
    public boolean isAdmin(Authentication authenticatedUser) {
        Authentication authentication = resolveAuthentication(authenticatedUser);
        String expected = "ROLE_ADMIN";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Long requireClientId(Authentication authenticatedUser) {
        Authentication authentication = resolveAuthentication(authenticatedUser);
        String email = EmailUtils.normalizeEmail(authentication.getName());

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (user.getClient() == null || user.getClient().getId() == null) {
            throw new AccessDeniedException(MSG_ACCESS_DENIED);
        }

        return user.getClient().getId();
    }

    private Authentication resolveAuthentication(Authentication authenticatedUser) {
        if (authenticatedUser != null) {
            return authenticatedUser;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication;
    }
}
