package com.app.civify.usecase.admin;

import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminUserUseCase {
    PagedResponse<UserResponseDTO> filterUsers(InputDataModel inputData);

    PagedResponse<UserResponseDTO> getAllUsers(int page, int size);

    UserResponseDTO getUserById(Long userId);

    UserResponseDTO getUserByIdWithStats(Long userId);

    UserResponseDTO updateUserLimits(Long userId, Integer cvLimit);

    UserResponseDTO toggleAccountLock(Long userId);

    UserResponseDTO updateUserGroup(Long userId, Long groupId);

    UserResponseDTO updateUserTokens(Long userId, Integer extraTokens);
}

