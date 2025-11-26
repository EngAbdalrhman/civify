package com.app.civify.service.user;

import com.app.civify.entity.User;

import java.util.List;

import com.app.civify.dto.user.UserDto;

public interface UserService {

    List<User> findAll();

    User findById(Long id);

    UserDto getMyUser();

    User getCurrentUser();

    long countTotalUsers();
}
