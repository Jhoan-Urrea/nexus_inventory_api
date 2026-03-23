package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.entity.Country;
import com.example.nexus.modules.location.entity.DepartmentRegion;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomUserDetailsService.class)
class CustomUserDetailsServiceIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameLoadsRolesInOneFlow() {
        City city = persistMinimalCity();
        Role adminRole = Role.builder().name("ADMIN").description("Admin").build();
        entityManager.persist(adminRole);

        AppUser user = AppUser.builder()
                .username("adminuser")
                .email("admin-int@test.nexus.local")
                .password("$2a$10$dummyhashfordetailservice")
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(Set.of(adminRole))
                .build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        UserDetails details = customUserDetailsService.loadUserByUsername("admin-int@test.nexus.local");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }

    private City persistMinimalCity() {
        Country country = Country.builder().name("TestCountry").build();
        entityManager.persist(country);
        DepartmentRegion region = DepartmentRegion.builder()
                .name("TestRegion")
                .country(country)
                .build();
        entityManager.persist(region);
        City city = City.builder()
                .name("TestCity")
                .departmentRegion(region)
                .build();
        entityManager.persist(city);
        return city;
    }
}
