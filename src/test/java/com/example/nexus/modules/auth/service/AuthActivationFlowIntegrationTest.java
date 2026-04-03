package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.ActivateAccountRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.model.AuthenticatedFlowResult;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthActivationFlowIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void activateAccountShouldPersistNewPasswordAndAllowSubsequentLogin() {
        City city = persistMinimalCity();
        Role userRole = Role.builder().name("USER").description("User").build();
        entityManager.persist(userRole);

        String email = "activation-flow@test.nexus.local";
        String token = UUID.randomUUID().toString();
        String initialHash = passwordEncoder.encode("Tmp!1InitialPassword");

        AppUser user = AppUser.builder()
                .username("activation-flow-user")
                .email(email)
                .password(initialHash)
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(Set.of(userRole))
                .activationToken(token)
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        String newPassword = "Str0ng!ActivationPass";

        AuthenticatedFlowResult activationResponse = authService.activateAccount(
                new ActivateAccountRequest(token, newPassword),
                "127.0.0.1"
        );

        assertEquals("Account activated successfully", activationResponse.message());
        assertEquals(email, activationResponse.email());

        AppUser activatedUser = appUserRepository.findByEmail(email).orElseThrow();
        assertNotEquals(initialHash, activatedUser.getPassword());
        assertTrue(passwordEncoder.matches(newPassword, activatedUser.getPassword()));
        assertFalse(activatedUser.isActivationRequired());
        assertFalse(activatedUser.isFirstLogin());
        assertNull(activatedUser.getActivationToken());
        assertNull(activatedUser.getActivationTokenExpiresAt());

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        assertTrue(passwordEncoder.matches(newPassword, userDetails.getPassword()));

        AuthTokens authTokens = authService.login(new LoginRequest(email, newPassword), "127.0.0.1");
        assertNotNull(authTokens.accessToken());
        assertNotNull(authTokens.refreshToken());
    }

    private City persistMinimalCity() {
        Country country = Country.builder().name("ActivationCountry").build();
        entityManager.persist(country);

        DepartmentRegion region = DepartmentRegion.builder()
                .name("ActivationRegion")
                .country(country)
                .build();
        entityManager.persist(region);

        City city = City.builder()
                .name("ActivationCity")
                .departmentRegion(region)
                .build();
        entityManager.persist(city);

        return city;
    }
}
