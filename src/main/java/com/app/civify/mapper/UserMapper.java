package com.app.civify.mapper;

import org.mapstruct.Mapper;

import com.app.civify.dto.user.UserDto;
import com.app.civify.entity.User;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {})
public interface UserMapper extends CommonMapper<User, UserDto> {

    @Override
    @Mapping(target = "cvCount", ignore = true)
    @Mapping(target = "apiUsageCount", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    UserDto toDto(User entity);

    @Override
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    @Mapping(target = "oauthProvider", ignore = true)
    @Mapping(target = "oauthSubject", ignore = true)
    @Mapping(target = "cvLimit", ignore = true)
    @Mapping(target = "isEmailVerified", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "extraTokens", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "preferredLanguage", ignore = true)
    @Mapping(target = "preferredTheme", ignore = true)
    @Mapping(target = "welcomeEmailSent", ignore = true)
    @Mapping(target = "groupName", ignore = true)
    User toEntity(UserDto dto);

}
