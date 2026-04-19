package com.example.nexus.modules.sales.security;

import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.user.constants.RoleConstants;
import com.example.nexus.modules.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OwnershipValidationServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OwnershipValidationServiceImpl ownershipValidationService;

    @Test
    void salesAgentShouldHaveElevatedSalesAccessWithoutBeingAdmin() {
        Authentication authentication = authenticationWith(RoleConstants.SALES_AGENT);

        assertTrue(ownershipValidationService.hasElevatedSalesAccess(authentication));
        assertFalse(ownershipValidationService.isAdmin(authentication));
    }

    @Test
    void validateReservationOwnershipShouldBypassClientChecksForSalesAgent() {
        Authentication authentication = authenticationWith(RoleConstants.SALES_AGENT);

        ownershipValidationService.validateReservationOwnership(99L, authentication);

        verifyNoInteractions(appUserRepository, reservationRepository, contractRepository, paymentRepository);
    }

    private Authentication authenticationWith(String roleName) {
        return new UsernamePasswordAuthenticationToken(
                "sales.agent@nexus.test",
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }
}
