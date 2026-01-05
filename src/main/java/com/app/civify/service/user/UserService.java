package com.app.civify.service.user;

import com.app.civify.entity.User;

import java.util.List;

import com.app.civify.dto.user.ProfileUpdateRequest;
import com.app.civify.dto.user.UserDto;

public interface UserService {

    List<User> findAll();

    User findById(Long id);

    UserDto getMyUser();

    UserDto updateMyUser(ProfileUpdateRequest request);

    User getCurrentUser();

    Long getCurrentUserId();

    long countTotalUsers();
}
