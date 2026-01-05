package com.app.civify.service.user;

import com.app.civify.dto.user.ProfileUpdateRequest;
import com.app.civify.dto.user.UserDto;
import com.app.civify.entity.User;
import com.app.civify.mapper.UserMapper;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
        return userMapper.toDto(userRepository.findByUsername(username).orElse(new User()));
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
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
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
}
