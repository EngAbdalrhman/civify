package com.app.civify.dto.user;

import com.app.civify.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User entity and UserResponseDTO
 */
@Component
public class UserMapper {
    
    /**
     * Convert User entity to UserResponseDTO
     * Excludes sensitive fields like password, apiKeyHash, and oauthSubject
     */
    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .phone(user.getPhone())
                .summary(user.getSummary())
                .experience(user.getExperience())
                .education(user.getEducation())
                .skills(user.getSkills())
                .isEmailVerified(user.getIsEmailVerified())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .oauthProvider(user.getOauthProvider())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .isDeleted(user.getIsDeleted())
                .build();
    }
    
    /**
     * Convert User entity to UserResponseDTO with subscription statistics
     */
    public UserResponseDTO toResponseDTOWithStats(User user, Long activeSubscriptionCount, Long totalSubscriptionCount) {
        UserResponseDTO dto = toResponseDTO(user);
        if (dto != null) {
            dto.setActiveSubscriptionCount(activeSubscriptionCount);
            dto.setTotalSubscriptionCount(totalSubscriptionCount);
        }
        return dto;
    }
}

