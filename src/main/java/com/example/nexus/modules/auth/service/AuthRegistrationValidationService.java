package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.user.entity.Role;

public interface AuthRegistrationValidationService {

    RegistrationContext validate(RegisterRequest request);

    record RegistrationContext(City city, Role defaultRole) {
    }
}
