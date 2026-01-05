package com.app.civify.usecase.admin;

import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.service.user.AdminUserService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserUseCaseImpl extends CommonService implements AdminUserUseCase {

    private final AdminUserService adminUserService;

    @Override
    public PagedResponse<UserResponseDTO> filterUsers(InputDataModel inputData) {
        log.debug("Filtering users with criteria: {}", inputData);
        return adminUserService.filterUsers(inputData);
    }

    @Override
    public PagedResponse<UserResponseDTO> getAllUsers(int page, int size) {
        return adminUserService.getAllUsers(page, size);
    }

    @Override
    public UserResponseDTO getUserById(Long userId) {
        return adminUserService.getUserById(userId);
    }

    @Override
    public UserResponseDTO getUserByIdWithStats(Long userId) {
        return adminUserService.getUserByIdWithStats(userId);
    }

    @Override
    public UserResponseDTO updateUserLimits(Long userId, Integer cvLimit) {
        return adminUserService.updateUserLimits(userId, cvLimit);
    }

    @Override
    public UserResponseDTO toggleAccountLock(Long userId) {
        return adminUserService.toggleAccountLock(userId);
    }

    @Override
    public UserResponseDTO updateUserGroup(Long userId, Long groupId) {
        return adminUserService.updateUserGroup(userId, groupId);
    }

    @Override
    public UserResponseDTO updateUserTokens(Long userId, Integer extraTokens) {
        return adminUserService.updateUserTokens(userId, extraTokens);
    }
}
