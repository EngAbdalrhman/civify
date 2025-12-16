package com.app.civify.dto.user;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for User entity
 * Excludes sensitive fields like password, apiKeyHash, and oauthSubject
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User response data transfer object")
public class UserResponseDTO {
    
    @Schema(description = "User ID", example = "1")
    private Long id;
    
    @Schema(description = "Username", example = "john_doe")
    private String username;
    
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;
    
    @Schema(description = "First name", example = "John")
    private String firstName;
    
    @Schema(description = "Last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "Phone number", example = "+1234567890")
    private String phoneNumber;
    
    @Schema(description = "Profile picture URL")
    private String profilePictureUrl;
    
    @Schema(description = "Phone (legacy field)", example = "+1234567890")
    private String phone;
    
    @Schema(description = "Professional summary")
    private String summary;
    
    @Schema(description = "Work experience")
    private String experience;
    
    @Schema(description = "Education background")
    private String education;
    
    @Schema(description = "Skills")
    private String skills;
    
    // REMOVED: private Role role;
    
    @Schema(description = "Email verification status", example = "true")
    private Boolean isEmailVerified;
    
    @Schema(description = "Two-factor authentication status", example = "false")
    private Boolean twoFactorEnabled;
    
    @Schema(description = "OAuth provider", example = "google")
    private String oauthProvider;
    
    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Created by user", example = "admin")
    private String createdBy;
    
    @Schema(description = "Last updated by user", example = "admin")
    private String updatedBy;
    
    @Schema(description = "Soft delete flag", example = "false")
    private Boolean isDeleted;
    
    // Statistics (for admin)
    @Schema(description = "Number of active subscriptions", example = "1")
    private Long activeSubscriptionCount;
    
    @Schema(description = "Total number of subscriptions", example = "3")
    private Long totalSubscriptionCount;

    @Schema(description = "Current subscription plan", example = "premium")
    private String plan;

    @Schema(description = "Number of CVs created", example = "2")
    private Integer cvCount;

    @Schema(description = "API usage count for current month", example = "150")
    private Integer apiUsageCount;

    @Schema(description = "Effective permissions for the user")
    private java.util.Set<String> permissions;
}

