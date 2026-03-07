package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
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

    private static final String DEFAULT_ROLE_NAME = "USER";
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    private final AppUserRepository appUserRepository;
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;

    @Override
    public RegistrationContext validate(RegisterRequest request) {

        if (appUserRepository.existsByEmail(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
        }

        validatePasswordPolicy(request.password());

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "City not found"));

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Default role USER not configured"));

        return new RegistrationContext(city, defaultRole);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");
        }

        if (!PASSWORD_LETTER_PATTERN.matcher(password).matches()
                || !PASSWORD_DIGIT_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include letters and numbers");
        }
    }
}
