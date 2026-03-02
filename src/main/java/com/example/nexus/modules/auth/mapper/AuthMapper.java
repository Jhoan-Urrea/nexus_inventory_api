package com.example.nexus.modules.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.auth.dto.RegisterRequest;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "status", expression = "java(UserStatus.ACTIVE)")
    AppUser toEntity(RegisterRequest request);
}
