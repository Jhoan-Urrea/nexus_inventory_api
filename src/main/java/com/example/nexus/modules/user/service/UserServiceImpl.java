package com.example.nexus.modules.user.service;

import com.example.nexus.modules.auth.service.PasswordPolicyService;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UpdateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.constants.RoleConstants;
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
    private static final String MSG_USER_NOT_FOUND = "User not found";
    private static final String MSG_INACTIVE_USER_UPDATE = "Inactive users cannot be edited";

    private final AppUserRepository userRepository;
    private final UserMapper userMapper;
    private final ClientRepository clientRepository;
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    public List<UserResponse> findAllUsers() {
        return userRepository.findAllWithRoles()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse findUserById(Long id) {
        return userRepository.findWithRolesById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));
    }

    @Override
    public UserResponse findCurrentUserByEmail(String email) {
        return userRepository.findWithRolesByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));
    }

    @Override
    public List<UserResponse> findUsersByClientId(Long clientId) {
        return userRepository.findWithRolesByClientId(clientId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already registered");
        }

        passwordPolicyService.validate(request.password());

        City city = loadCity(request.cityId());
        Set<Role> roles = resolveRoles(request.roles());

        AppUser user = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .city(city)
                .roles(roles)
                .build();

        user.setClient(resolveClient(request.clientId()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, String actorEmail) {
        AppUser actor = requireUserByEmail(actorEmail);

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_INACTIVE_USER_UPDATE);
        }

        if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already registered");
        }

        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                });

        Set<Role> requestedRoles = resolveRoles(request.roles());
        validateSelfAdministrationGuards(actor, user, request.status(), requestedRoles);

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setCity(loadCity(request.cityId()));
        user.setClient(resolveClient(request.clientId()));
        user.setRoles(requestedRoles);
        user.setStatus(request.status());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id, String actorEmail) {
        AppUser actor = requireUserByEmail(actorEmail);

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));

        if (actor.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators cannot delete themselves");
        }

        if (user.getStatus() != UserStatus.INACTIVE) {
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
        }
    }

    private City loadCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));
    }

    private Client resolveClient(Long clientId) {
        if (clientId == null) {
            return null;
        }

        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {

        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Role not found: " + roleName
                        )))
                .collect(Collectors.toSet());
    }

    private AppUser requireUserByEmail(String email) {
        return userRepository.findWithRolesByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Actor user not found"));
    }

    private void validateSelfAdministrationGuards(
            AppUser actor,
            AppUser targetUser,
            UserStatus requestedStatus,
            Set<Role> requestedRoles
    ) {
        if (!actor.getId().equals(targetUser.getId())) {
            return;
        }
        if (!hasRole(requestedRoles, RoleConstants.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators cannot remove their own ADMIN role");
        }
        if (requestedStatus != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators cannot deactivate or block themselves");
        }
    }

    private boolean hasRole(Set<Role> roles, String roleName) {
        return roles.stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }
}
