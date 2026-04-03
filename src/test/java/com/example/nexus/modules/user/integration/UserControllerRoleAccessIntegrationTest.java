package com.example.nexus.modules.user.integration;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.location.repository.CountryRepository;
import com.example.nexus.modules.location.repository.DepartmentRegionRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
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
class UserControllerRoleAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private DepartmentRegionRepository departmentRegionRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void adminShouldReturnAllUsers() throws Exception {
        UserAccessFixture fixture = persistFixture();

        mockMvc.perform(get("/api/users")
                        .with(user(fixture.admin().getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void salesAgentShouldReturnOnlyOwnCreatedUsers() throws Exception {
        UserAccessFixture fixture = persistFixture();

        mockMvc.perform(get("/api/users")
                        .with(user(fixture.salesAgent().getEmail()).roles("SALES_AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("client.one@nexus.local"))
                .andExpect(jsonPath("$[1].email").value("client.two@nexus.local"));
    }

    @Test
    void salesAgentShouldReceiveForbiddenWhenConsultingAnotherCreator() throws Exception {
        UserAccessFixture fixture = persistFixture();

        mockMvc.perform(get("/api/users/created-by/{id}", fixture.otherSalesAgent().getId())
                        .with(user(fixture.salesAgent().getEmail()).roles("SALES_AGENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Sales agents can only access users created by themselves"));
    }

    @Test
    void adminShouldBeAbleToConsultUsersCreatedByAnyAgent() throws Exception {
        UserAccessFixture fixture = persistFixture();

        mockMvc.perform(get("/api/users/created-by/{id}", fixture.salesAgent().getId())
                        .with(user(fixture.admin().getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("client.one@nexus.local"))
                .andExpect(jsonPath("$[1].email").value("client.two@nexus.local"));
    }

    private UserAccessFixture persistFixture() {
        City city = persistCityGraph();
        Role adminRole = persistRole("ADMIN");
        Role salesAgentRole = persistRole("SALES_AGENT");
        Role clientRole = persistRole("CLIENT");

        AppUser admin = persistUser("admin@nexus.local", adminRole, city, null);
        AppUser salesAgent = persistUser("agent.one@nexus.local", salesAgentRole, city, admin.getId());
        AppUser otherSalesAgent = persistUser("agent.two@nexus.local", salesAgentRole, city, admin.getId());
        persistUser("client.one@nexus.local", clientRole, city, salesAgent.getId());
        persistUser("client.two@nexus.local", clientRole, city, salesAgent.getId());
        persistUser("client.three@nexus.local", clientRole, city, otherSalesAgent.getId());

        return new UserAccessFixture(admin, salesAgent, otherSalesAgent);
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

    private Role persistRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private AppUser persistUser(String email, Role role, City city, Long createdBy) {
        return appUserRepository.save(AppUser.builder()
                .username(email)
                .email(email)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(Set.of(role))
                .createdBy(createdBy)
                .build());
    }

    private record UserAccessFixture(
            AppUser admin,
            AppUser salesAgent,
            AppUser otherSalesAgent
    ) {
    }
}
