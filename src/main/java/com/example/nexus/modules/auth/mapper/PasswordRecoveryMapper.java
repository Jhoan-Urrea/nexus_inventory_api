package com.example.nexus.modules.auth.mapper;

import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.entity.PasswordResetToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PasswordRecoveryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "used", constant = "false")
    @Mapping(target = "attemptCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    PasswordResetToken toEntity(ForgotPasswordRequest request);
}
