package com.example.nexus.modules.auth.mapper;

import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.user.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "status", expression = "java(com.example.nexus.modules.user.entity.UserStatus.ACTIVE)")
    AppUser toEntity(RegisterRequest request);
}
