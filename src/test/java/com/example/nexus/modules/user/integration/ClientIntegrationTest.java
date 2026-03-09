package com.example.nexus.modules.user.integration;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.location.repository.CountryRepository;
import com.example.nexus.modules.location.repository.DepartmentRegionRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.ClientRepository;
import com.example.nexus.modules.user.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private DepartmentRegionRepository departmentRegionRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldReturnAllUsersForAClient() throws Exception {
        City city = persistCityGraph();
        Client client = clientRepository.save(Client.builder().name("Acme").build());
        Role clientRole = roleRepository.save(Role.builder().name("CLIENT").build());

        appUserRepository.save(AppUser.builder()
                .username("cliente1")
                .email("cliente1@empresa.com")
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(Set.of(clientRole))
                .build());

        appUserRepository.save(AppUser.builder()
                .username("cliente2")
                .email("cliente2@empresa.com")
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(Set.of(clientRole))
                .build());

        mockMvc.perform(get("/clients/{id}/users", client.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clientId").value(client.getId()))
                .andExpect(jsonPath("$[1].clientId").value(client.getId()));
    }

    private City persistCityGraph() {
        Country country = countryRepository.save(Country.builder().name("Colombia").build());
        DepartmentRegion region = departmentRegionRepository.save(DepartmentRegion.builder()
                .name("Bogota DC")
                .country(country)
                .build());

        return cityRepository.save(City.builder()
                .name("Bogota")
                .departmentRegion(region)
                .build());
    }
}
