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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldAllowOneClientWithMultipleUsers() {
        City city = persistCityGraph();
        Client client = persistClient("Acme");

        AppUser user1 = persistUser("cliente1", "cliente1@example.com", city, client);
        AppUser user2 = persistUser("cliente2", "cliente2@example.com", city, client);

        client.getUsers().add(user1);
        client.getUsers().add(user2);
        entityManager.persist(client);
        entityManager.flush();
        entityManager.clear();

        Client loaded = clientRepository.findById(client.getId()).orElseThrow();

        assertNotNull(loaded);
        assertEquals(2, loaded.getUsers().size());
    }

    @Test
    void deletingClientShouldUnlinkUsersAndKeepThemPersisted() {
        City city = persistCityGraph();
        Client client = persistClient("Globex");

        AppUser user1 = persistUser("c1", "c1@example.com", city, client);
        AppUser user2 = persistUser("c2", "c2@example.com", city, client);

        client.getUsers().addAll(List.of(user1, user2));
        entityManager.persist(client);
        entityManager.flush();

        Long user1Id = user1.getId();
        Long user2Id = user2.getId();
        Long clientId = client.getId();

        clientRepository.deleteById(clientId);
        entityManager.flush();
        entityManager.clear();

        AppUser persisted1 = appUserRepository.findById(user1Id).orElseThrow();
        AppUser persisted2 = appUserRepository.findById(user2Id).orElseThrow();

        assertNull(persisted1.getClient());
        assertNull(persisted2.getClient());
    }

    private City persistCityGraph() {
        Country country = Country.builder().name("Colombia").build();
        entityManager.persist(country);

        DepartmentRegion region = DepartmentRegion.builder()
                .name("Bogota")
                .country(country)
                .build();
        entityManager.persist(region);

        City city = City.builder()
                .name("Bogota")
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

    private AppUser persistUser(String username, String email, City city, Client client) {
        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .build();
        entityManager.persist(user);
        return user;
    }
}
