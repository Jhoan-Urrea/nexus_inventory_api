package com.example.nexus.modules.user.repository;

import com.example.nexus.modules.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<AppUser> findWithRolesByEmail(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<AppUser> findWithRolesById(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles")
    List<AppUser> findAllWithRoles();

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles WHERE u.client.id = :clientId")
    List<AppUser> findWithRolesByClientId(@Param("clientId") Long clientId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    List<AppUser> findByClientId(Long clientId);
}