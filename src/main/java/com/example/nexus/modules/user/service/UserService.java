package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UpdateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.location.entity.City;

import java.util.List;

public interface UserService {

    List<AppUser> getAllUsers();

    List<AppUser> getUsersCreatedBy(Long userId);

    List<UserResponse> findAllUsers();

    UserResponse findUserById(Long id);

    UserResponse findCurrentUserByEmail(String email);

    List<UserResponse> findUsersByClientId(Long clientId);

    UserResponse createUser(CreateUserRequest request);

    AppUser createPendingClientUser(Client client, City city);

    UserResponse updateUser(Long id, UpdateUserRequest request, String actorEmail);

    void deleteUser(Long id, String actorEmail);

    /**
     * Reactivación explícita: solo {@code INACTIVE} → {@code ACTIVE}.
     */
    UserResponse activateUser(Long id, String actorEmail);

    /**
     * Desactivación explícita (misma semántica que borrado lógico): pasa a {@code INACTIVE}.
     */
    UserResponse deactivateUser(Long id, String actorEmail);
}
