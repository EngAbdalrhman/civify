package com.app.civify.mapper;

import org.mapstruct.Mapper;

import com.app.civify.dto.user.UserDto;
import com.app.civify.entity.User;

@Mapper(componentModel = "spring", uses = {})
public interface UserMapper extends CommonMapper<User, UserDto> {

    @Override
    @org.mapstruct.Mapping(target = "cvCount", ignore = true)
    @org.mapstruct.Mapping(target = "apiUsageCount", ignore = true)
    @org.mapstruct.Mapping(target = "permissions", ignore = true)
    UserDto toDto(User entity);

    @Override
    @org.mapstruct.Mapping(target = "password", ignore = true)
    @org.mapstruct.Mapping(target = "group", ignore = true)
    @org.mapstruct.Mapping(target = "subscriptions", ignore = true)
    @org.mapstruct.Mapping(target = "apiKeyHash", ignore = true)
    @org.mapstruct.Mapping(target = "oauthProvider", ignore = true)
    @org.mapstruct.Mapping(target = "oauthSubject", ignore = true)
    @org.mapstruct.Mapping(target = "cvLimit", ignore = true)
    @org.mapstruct.Mapping(target = "isEmailVerified", ignore = true)
    @org.mapstruct.Mapping(target = "twoFactorEnabled", ignore = true)
    @org.mapstruct.Mapping(target = "lastLoginAt", ignore = true)
    User toEntity(UserDto dto);

}
