package com.app.civify.mapper;

import org.mapstruct.Mapper;

import com.app.civify.dto.user.UserDto;
import com.app.civify.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper extends CommonMapper<User, UserDto> {

}
