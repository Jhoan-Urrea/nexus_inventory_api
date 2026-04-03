package com.example.nexus.modules.user.repository;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.testsupport.ClientTestFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void findByClientIdShouldReturnOnlyUsersForThatClient() {
        City city = persistCityGraph();
        Client clientA = persistClient("Client A");
        Client clientB = persistClient("Client B");

        persistUser("a1", "a1@example.com", city, clientA);
        persistUser("a2", "a2@example.com", city, clientA);
        persistUser("b1", "b1@example.com", city, clientB);

        entityManager.flush();
        entityManager.clear();

        List<AppUser> clientAUsers = appUserRepository.findByClientId(clientA.getId());

        assertEquals(2, clientAUsers.size());
        assertTrue(clientAUsers.stream().allMatch(u -> u.getClient() != null && u.getClient().getId().equals(clientA.getId())));
    }

    @Test
    void findByActivationTokenShouldReturnPersistedActivationData() {
        City city = persistCityGraph();
        String activationToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(86400);

        AppUser user = AppUser.builder()
                .username("activable")
                .email("activable@example.com")
                .password("secret")
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(java.util.Collections.emptySet())
                .activationToken(activationToken)
                .activationTokenExpiresAt(expiresAt)
                .activationRequired(true)
                .firstLogin(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        Optional<AppUser> result = appUserRepository.findByActivationToken(activationToken);

        assertTrue(result.isPresent());
        assertEquals(activationToken, result.get().getActivationToken());
        assertEquals(expiresAt.getEpochSecond(), result.get().getActivationTokenExpiresAt().getEpochSecond());
        assertTrue(result.get().isActivationRequired());
        assertTrue(result.get().isFirstLogin());
        assertNotNull(result.get().getId());
    }

    @Test
    void findByCreatedByShouldReturnOnlyUsersCreatedByThatUser() {
        City city = persistCityGraph();
        Client client = persistClient("Client Trace");

        persistUser("creator-a-1", "creator-a-1@example.com", city, client, 100L);
        persistUser("creator-a-2", "creator-a-2@example.com", city, client, 100L);
        persistUser("creator-b-1", "creator-b-1@example.com", city, client, 200L);

        entityManager.flush();
        entityManager.clear();

        List<AppUser> result = appUserRepository.findByCreatedBy(100L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> Long.valueOf(100L).equals(user.getCreatedBy())));
    }

    private City persistCityGraph() {
        Country country = Country.builder().name("Colombia").build();
        entityManager.persist(country);

        DepartmentRegion region = DepartmentRegion.builder()
                .name("Antioquia")
                .country(country)
                .build();
        entityManager.persist(region);

        City city = City.builder()
                .name("Medellin")
                .departmentRegion(region)
                .build();
        entityManager.persist(city);
        return city;
    }

    private Client persistClient(String name) {
        Client client = ClientTestFixtures.newClient(name);
        entityManager.persist(client);
        return client;
    }

    private void persistUser(String username, String email, City city, Client client) {
        persistUser(username, email, city, client, null);
    }

    private void persistUser(String username, String email, City city, Client client, Long createdBy) {
        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .createdBy(createdBy)
                .roles(java.util.Collections.emptySet())
                .build();
        entityManager.persist(user);
    }
}
