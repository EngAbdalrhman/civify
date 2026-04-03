package com.app.civify.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User dto data transfer object")
public class UserDto {

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

    @Schema(description = "Number of CVs created", example = "2")
    private Integer cvCount;

    @Schema(description = "API usage count for current month", example = "150")
    private Integer apiUsageCount;

    @Schema(description = "List of user permissions")
    private java.util.Set<String> permissions;

    @Schema(description = "OAuth Provider (GOOGLE, LINKEDIN, etc.)")
    private String oauthProvider;

    @Schema(description = "OAuth Subject (User ID from Provider)")
    private String oauthSubject;

    @Schema(description = "User's referral code", example = "A1B2C3D4")
    private String referralCode;

    @Schema(description = "Number of users referred by this user", example = "5")
    private Integer referralCount;

    @Schema(description = "Whether the general onboarding is completed")
    private Boolean onboardingCompleted;

    @Schema(description = "JSON representing completed tours status")
    private String completedTours;

}
