package com.app.civify.service.user;

import com.app.civify.dto.user.OnboardingStatusRequest;
import com.app.civify.dto.user.ProfileUpdateRequest;
import com.app.civify.dto.user.UserDto;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.mapper.UserMapper;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public UserDto getMyUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        String username = auth.getName();
        return userMapper.toDto(userRepository.findActiveByUsername(username).orElse(new User()));
    }

    @Override
    @Transactional
    public UserDto updateMyUser(ProfileUpdateRequest request, UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile updated for: {}", user.getUsername());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateOnboardingStatus(OnboardingStatusRequest request, UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        if (request.getOnboardingCompleted() != null) {
            user.setOnboardingCompleted(request.getOnboardingCompleted());
        }
        if (request.getCompletedTours() != null) {
            user.setCompletedTours(request.getCompletedTours());
        }
        if (request.getPrimaryIntent() != null) {
            user.setPrimaryIntent(request.getPrimaryIntent());
        }

        // Use custom query to bypass audit listeners for onboarding updates
        userRepository.updateUserOnboarding(
                user.getId(),
                user.getOnboardingCompleted(),
                user.getCompletedTours(),
                user.getPrimaryIntent()
        );
        log.info("User onboarding status updated for: {}", user.getUsername());
        return userMapper.toDto(user);
    }

    @Override
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        String username = auth.getName();
        return userRepository.findActiveByUsername(username).orElse(null);
    }

    @Override
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    @Override
    public long countTotalUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional
    public UserDto updateProfilePicture(Long userId, String base64Image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setProfilePictureUrl(base64Image);
        User savedUser = userRepository.save(user);
        log.info("Profile picture updated for user: {}", user.getUsername());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteMyAccount(UserPrincipal userPrincipal, String password) {
        User user = userPrincipal.getUser();
        if (user == null) {
            throw new IllegalStateException("User not authenticated");
        }

        boolean isOAuthAccount = StringUtils.hasText(user.getOauthProvider());

        // Password confirmation is required only for local (non-OAuth) accounts.
        if (!isOAuthAccount && (!StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPassword()))) {
            throw new BadRequestException("user.account.delete.password.incorrect");
        }

        // Soft delete the user
        user.setIsDeleted(true);
        userRepository.save(user);
        log.info("Account soft deleted for user: {}", user.getUsername());
    }
}
