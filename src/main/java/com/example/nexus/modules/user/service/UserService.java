package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UpdateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> findAllUsers();

    UserResponse findUserById(Long id);

    UserResponse findCurrentUserByEmail(String email);

    List<UserResponse> findUsersByClientId(Long clientId);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}