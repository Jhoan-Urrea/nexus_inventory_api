package com.example.nexus.modules.user.mapper;

import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "clientId", expression = "java(user.getClient() != null ? user.getClient().getId() : null)")
    UserResponse toResponse(AppUser user);
}
