package com.example.nexus.modules.user.repository;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Client client = Client.builder().name(name).build();
        entityManager.persist(client);
        return client;
    }

    private void persistUser(String username, String email, City city, Client client) {
        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .build();
        entityManager.persist(user);
    }
}
