package com.example.nexus.modules.user.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.mapper.UserMapper;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.ClientRepository;
import com.example.nexus.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository userRepository;
    private final UserMapper userMapper;
    private final ClientRepository clientRepository;
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public List<UserResponse> findUsersByClientId(Long clientId) {
        return userRepository.findByClientId(clientId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse findCurrentUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));

        Set<Role> roles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + roleName)))
                .collect(Collectors.toSet());

        AppUser user = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(roles)
                .build();

        if (request.clientId() != null) {
            Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            user.setClient(client);
        }

        AppUser savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }
}
