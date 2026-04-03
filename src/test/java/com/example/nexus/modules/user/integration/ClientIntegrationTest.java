package com.example.nexus.modules.user.integration;

import com.example.nexus.modules.auth.service.AccountActivationEmailService;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.nexus.testsupport.ClientTestFixtures;
import static org.mockito.Mockito.verify;

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

    @MockitoBean
    private AccountActivationEmailService accountActivationEmailService;

    @Test
    void shouldReturnAllUsersForAClient() throws Exception {
        City city = persistCityGraph();
        Client client = clientRepository.save(ClientTestFixtures.newClient("Acme"));
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

        mockMvc.perform(get("/api/clients/{clientId}/users", client.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clientId").value(client.getId()))
                .andExpect(jsonPath("$[1].clientId").value(client.getId()));
    }

    @Test
    void shouldCreateClientAndProvisionPendingActivationUser() throws Exception {
        City city = persistCityGraph();
        roleRepository.save(Role.builder().name("CLIENT").build());
        AppUser admin = persistAuthenticatedUser("admin@nexus.local", "ADMIN", city);

        mockMvc.perform(post("/api/clients")
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cliente Demo",
                                  "email": "CLIENTE.DEMO@NEXUS.LOCAL",
                                  "phone": "3000000000",
                                  "documentType": "NIT",
                                  "documentNumber": "900000999",
                                  "businessName": "Cliente Demo SAS",
                                  "address": "Bogota",
                                  "cityId": %d
                                }
                                """.formatted(city.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cliente.demo@nexus.local"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Client savedClient = clientRepository.findAllByOrderByNameAsc()
                .stream()
                .filter(client -> "cliente.demo@nexus.local".equals(client.getEmail()))
                .findFirst()
                .orElseThrow();

        AppUser savedUser = appUserRepository.findByEmailIgnoreCase("cliente.demo@nexus.local")
                .orElseThrow();

        assertEquals(savedClient.getId(), savedUser.getClient().getId());
        assertEquals("cliente.demo@nexus.local", savedUser.getEmail());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals(admin.getId(), savedClient.getCreatedBy());
        assertEquals(admin.getId(), savedUser.getCreatedBy());
        assertEquals(true, savedUser.isActivationRequired());
        assertEquals(true, savedUser.isFirstLogin());
        assertNotNull(savedUser.getActivationToken());
        assertNotNull(savedUser.getActivationTokenExpiresAt());

        verify(accountActivationEmailService).sendAccountActivationEmail(
                "cliente.demo@nexus.local",
                savedUser.getActivationToken()
        );
    }

    @Test
    void shouldRejectClientCreationWhenCityDoesNotExist() throws Exception {
        City city = persistCityGraph();
        AppUser admin = persistAuthenticatedUser("admin@nexus.local", "ADMIN", city);

        mockMvc.perform(post("/api/clients")
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cliente Demo",
                                  "email": "cliente.demo@nexus.local",
                                  "phone": "3000000000",
                                  "documentType": "NIT",
                                  "documentNumber": "900000999",
                                  "businessName": "Cliente Demo SAS",
                                  "address": "Bogota",
                                  "cityId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("City not found"))
                .andExpect(jsonPath("$.path").value("/api/clients"));
    }

    @Test
    void shouldRejectClientCreationWhenEmailAlreadyExistsInAppUser() throws Exception {
        City city = persistCityGraph();
        Role clientRole = roleRepository.save(Role.builder().name("CLIENT").build());
        AppUser admin = persistAuthenticatedUser("admin@nexus.local", "ADMIN", city);

        appUserRepository.save(AppUser.builder()
                .username("existing-user")
                .email("cliente.demo@nexus.local")
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(Set.of(clientRole))
                .build());

        mockMvc.perform(post("/api/clients")
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cliente Demo",
                                  "email": "cliente.demo@nexus.local",
                                  "phone": "3000000000",
                                  "documentType": "NIT",
                                  "documentNumber": "900000999",
                                  "businessName": "Cliente Demo SAS",
                                  "address": "Bogota",
                                  "cityId": %d
                                }
                                """.formatted(city.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already in use"))
                .andExpect(jsonPath("$.path").value("/api/clients"));

        assertEquals(0, clientRepository.findAllByOrderByNameAsc().stream()
                .filter(clientEntity -> "cliente.demo@nexus.local".equals(clientEntity.getEmail()))
                .count());
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

    private AppUser persistAuthenticatedUser(String email, String roleName, City city) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        return appUserRepository.save(AppUser.builder()
                .username(email)
                .email(email)
                .password("123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(Set.of(role))
                .build());
    }
}
