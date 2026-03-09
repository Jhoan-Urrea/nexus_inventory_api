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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientAppUserRelationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldValidateClientOneToManyAppUserFlow() {
        City city = persistCityGraph();
        Client client = clientRepository.save(Client.builder().name("Cliente Demo").build());

        AppUser u1 = AppUser.builder()
                .username("cliente1")
                .email("cliente1@empresa.com")
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .build();

        AppUser u2 = AppUser.builder()
                .username("cliente2")
                .email("cliente2@empresa.com")
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .build();

        appUserRepository.saveAll(List.of(u1, u2));
        entityManager.flush();
        entityManager.clear();

        List<AppUser> usersByClient = appUserRepository.findByClientId(client.getId());
        assertEquals(2, usersByClient.size());
        assertTrue(usersByClient.stream().allMatch(u -> u.getClient() != null && u.getClient().getId().equals(client.getId())));

        clientRepository.deleteById(client.getId());
        entityManager.flush();
        entityManager.clear();

        AppUser persistedU1 = appUserRepository.findByEmail("cliente1@empresa.com").orElseThrow();
        AppUser persistedU2 = appUserRepository.findByEmail("cliente2@empresa.com").orElseThrow();

        assertNull(persistedU1.getClient());
        assertNull(persistedU2.getClient());
    }

    private City persistCityGraph() {
        Country country = Country.builder().name("Colombia").build();
        entityManager.persist(country);

        DepartmentRegion region = DepartmentRegion.builder()
                .name("Bogota DC")
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
}
