package com.app.civify.service.user;

import com.app.civify.entity.User;

import java.util.List;

import com.app.civify.dto.user.ProfileUpdateRequest;
import com.app.civify.dto.user.UserDto;
import com.app.civify.security.UserPrincipal;

public interface UserService {

    List<User> findAll();

    User findById(Long id);

    UserDto getMyUser();

    UserDto updateMyUser(ProfileUpdateRequest request, UserPrincipal userPrincipal);

    UserDto updateOnboardingStatus(com.app.civify.dto.user.OnboardingStatusRequest request, UserPrincipal userPrincipal);

    User getCurrentUser();

    Long getCurrentUserId();

    long countTotalUsers();

    UserDto updateProfilePicture(Long userId, String base64Image);

    void deleteMyAccount(UserPrincipal userPrincipal, String password);
}
