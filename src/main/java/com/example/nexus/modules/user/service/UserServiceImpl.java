package com.example.nexus.modules.user.service;

import com.example.nexus.exception.BusinessException;
import com.example.nexus.exception.NotFoundException;
import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.auth.service.AccountActivationEmailService;
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
import com.example.nexus.util.EmailUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String MSG_USER_NOT_FOUND = "User not found";
    private static final String MSG_INACTIVE_USER_UPDATE = "Inactive users cannot be edited";
    private static final long ACTIVATION_TOKEN_VALIDITY_HOURS = 24;
    private static final int USERNAME_MAX_LENGTH = 100;

    private final AppUserRepository userRepository;
    private final UserMapper userMapper;
    private final ClientRepository clientRepository;
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AccountActivationEmailService accountActivationEmailService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (!currentUserProvider.hasRole(RoleConstants.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("Fetching all users by ADMIN {}", currentUserId);
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getUsersCreatedBy(Long userId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        boolean isAdmin = currentUserProvider.hasRole(RoleConstants.ADMIN);
        boolean isSalesAgent = currentUserProvider.hasRole(RoleConstants.SALES_AGENT);

        if (!isAdmin && !isSalesAgent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (!isAdmin && !currentUserId.equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Sales agents can only access users created by themselves"
            );
        }

        log.info("Fetching users created by {}", userId);
        return userRepository.findByCreatedBy(userId);
    }

    @Override
    public List<UserResponse> findAllUsers() {
        return getAllUsers()
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
        return userRepository.findWithRolesByEmailIgnoreCase(EmailUtils.normalizeEmail(email))
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException(MSG_USER_NOT_FOUND));
    }

    @Override
    public List<UserResponse> findUsersByClientId(Long clientId) {
        return userRepository.findWithRolesByClientId(clientId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeRequiredEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException("Email already in use");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already in use");
        }

        City city = loadCity(request.cityId());
        Set<Role> roles = resolveRoles(request.roles());
        AppUser user = buildPendingActivationUser(
                request.username(),
                normalizedEmail,
                city,
                resolveClient(request.clientId()),
                roles
        );

        AppUser savedUser = savePendingActivationUser(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public AppUser createPendingClientUser(Client client, City city) {
        if (client == null) {
            throw new ValidationException("Client is required");
        }
        if (city == null) {
            throw new ValidationException("City is required");
        }

        String normalizedEmail = normalizeRequiredEmail(client.getEmail());

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(existing -> {
                    throw new BusinessException("Email already in use");
                });

        AppUser user = buildPendingActivationUser(
                generateAvailableClientUsername(normalizedEmail),
                normalizedEmail,
                city,
                client,
                Set.of(loadRole(RoleConstants.CLIENT))
        );

        return savePendingActivationUser(user);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, String actorEmail) {
        AppUser actor = requireUserByEmail(actorEmail);
        String normalizedEmail = normalizeRequiredEmail(request.email());

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_INACTIVE_USER_UPDATE);
        }

        if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new BusinessException("Username already in use");
        }

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Email already in use");
                });

        Set<Role> requestedRoles = resolveRoles(request.roles());
        validateSelfAdministrationGuards(actor, user, request.status(), requestedRoles);

        user.setUsername(request.username());
        user.setEmail(normalizedEmail);
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
        if (cityId == null || cityId <= 0) {
            throw new ValidationException("cityId is required");
        }

        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NotFoundException("City not found"));
    }

    private Client resolveClient(Long clientId) {
        if (clientId == null) {
            return null;
        }

        return clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found"));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {

        return roleNames.stream()
                .map(this::loadRole)
                .collect(Collectors.toSet());
    }

    private Role loadRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ValidationException("Role not found: " + roleName));
    }

    private AppUser requireUserByEmail(String email) {
        return userRepository.findWithRolesByEmailIgnoreCase(EmailUtils.normalizeEmail(email))
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

    private AppUser buildPendingActivationUser(
            String username,
            String email,
            City city,
            Client client,
            Set<Role> roles
    ) {
        String temporaryPassword = generateTemporaryPassword();
        String activationToken = UUID.randomUUID().toString();
        Instant activationTokenExpiresAt = Instant.now().plus(ACTIVATION_TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);

        passwordPolicyService.validate(temporaryPassword);

        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(roles)
                .activationToken(activationToken)
                .activationTokenExpiresAt(activationTokenExpiresAt)
                .activationRequired(true)
                .firstLogin(true)
                .createdBy(currentUserProvider.getCurrentUserId())
                .build();

        user.setClient(client);
        return user;
    }

    private AppUser savePendingActivationUser(AppUser user) {
        AppUser savedUser = userRepository.save(user);
        log.info(
                "User created userId={}, email={}, clientId={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getClient() != null ? savedUser.getClient().getId() : null
        );
        sendActivationEmailSafely(savedUser);
        return savedUser;
    }

    private void sendActivationEmailSafely(AppUser savedUser) {
        try {
            accountActivationEmailService.sendAccountActivationEmail(
                    savedUser.getEmail(),
                    savedUser.getActivationToken()
            );
        } catch (RuntimeException ex) {
            log.error("Unable to send account activation email to {}", savedUser.getEmail(), ex);
        }
    }

    private String generateAvailableClientUsername(String normalizedEmail) {
        if (normalizedEmail.length() <= USERNAME_MAX_LENGTH && !userRepository.existsByUsername(normalizedEmail)) {
            return normalizedEmail;
        }

        String localPart = normalizedEmail.contains("@")
                ? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
                : "client";
        String base = truncateUsername(localPart.isBlank() ? "client" : localPart, USERNAME_MAX_LENGTH - 9);

        String candidate;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            candidate = truncateUsername(base, USERNAME_MAX_LENGTH - 9) + "-" + suffix;
        } while (userRepository.existsByUsername(candidate));

        return candidate;
    }

    private String truncateUsername(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeRequiredEmail(String email) {
        String normalizedEmail = EmailUtils.normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ValidationException("Email is required");
        }
        return normalizedEmail;
    }

    private String generateTemporaryPassword() {
        return "Tmp!1" + UUID.randomUUID().toString().replace("-", "");
    }

}
