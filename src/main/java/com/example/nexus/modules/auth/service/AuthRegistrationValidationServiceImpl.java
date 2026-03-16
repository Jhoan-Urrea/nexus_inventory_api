package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.user.constants.RoleConstants;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthRegistrationValidationServiceImpl implements AuthRegistrationValidationService {

    private static final String DEFAULT_ROLE_NAME = RoleConstants.WAREHOUSE_EMPLOYEE;
    private final AppUserRepository appUserRepository;
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    public RegistrationContext validate(RegisterRequest request) {

        if (appUserRepository.existsByEmail(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (appUserRepository.existsByUsername(request.username())) {
            throw new AuthException(HttpStatus.CONFLICT, "Username already registered");
        }

        passwordPolicyService.validate(request.password());

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "City not found"));

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Default role " + DEFAULT_ROLE_NAME + " not configured"));

        return new RegistrationContext(city, defaultRole);
    }

}
