package com.app.civify.service.user;

import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

/**
 * Service interface for admin user management operations
 * Provides filtering, searching, and user management for administrators
 */
public interface AdminUserService {
    
    /**
     * Filter users with pagination and sorting
     * 
     * @param inputData Filter criteria, sorting, and pagination parameters
     * @return Paged response containing user DTOs
     */
    PagedResponse<UserResponseDTO> filterUsers(InputDataModel inputData);

    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User response DTO
     */
    UserResponseDTO getUserById(Long userId);
    
    /**
     * Get user by ID with subscription statistics
     * 
     * @param userId User ID
     * @return User response DTO with statistics
     */
    UserResponseDTO getUserByIdWithStats(Long userId);
    /**
     * Update user limits (CV)
     * 
     * @param userId User ID
     * @param cvLimit Custom CV limit (null to use plan limit)
     * @return Updated user DTO
     */
    UserResponseDTO updateUserLimits(Long userId, Integer cvLimit);

    /**
     * Toggle user account lock status (ban/unban)
     * 
     * @param userId User ID
     * @return Updated user DTO
     */
    UserResponseDTO toggleAccountLock(Long userId);

    /**
     * Update user's permission group
     * 
     * @param userId User ID
     * @param groupId Group ID to assign
     * @return Updated user DTO
     */
    UserResponseDTO updateUserGroup(Long userId, Long groupId);

    /**
     * Update user's extra tokens (wallet)
     * 
     * @param userId User ID
     * @param extraTokens Extra tokens to set
     * @return Updated user DTO
     */
    UserResponseDTO updateUserTokens(Long userId, Integer extraTokens);
}
