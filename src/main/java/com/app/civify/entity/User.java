package com.app.civify.entity;

import com.app.civify.security.model.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(length = 1000)
    private String summary;

    @Column(length = 1000)
    private String experience;

    @Column(length = 1000)
    private String education;

    @Column(length = 2000)
    private String skills;

    // Admin and License flags
    @Column(nullable = false)
    private Boolean isAdmin = false;

    @Column(nullable = false)
    private Boolean isLicensed = false;

    @Column(length = 50)
    private String licenseKey;

    @Column(name = "license_activated_at")
    private LocalDateTime licenseActivatedAt;

    // API Usage tracking
    @Column(length = 255)
    private String apiKeyHash;

    @Column(length = 10, name = "api_usage_month")
    private String apiUsageMonth;

    @Column(name = "api_usage_count", nullable = false)
    private Integer apiUsageCount = 0;

    @Column(name = "api_rate_limit", nullable = false)
    private Integer apiRateLimit = 500;

    // Legacy CV tracking (deprecated but kept for compatibility)
    @Column(name = "cv_count", nullable = false)
    private Integer cvCount = 0;

    @Column(name = "cv_limit", nullable = false)
    private Integer cvLimit = 3;

    // UI CV tracking (current month)
    @Column(name = "ui_cv_count", nullable = false)
    private Integer uiCvCount = 0;

    @Column(name = "ui_cv_limit", nullable = false)
    private Integer uiCvLimit = 5;

    @Column(length = 10, name = "ui_cv_month")
    private String uiCvMonth;

    // Subscription plan
    @Column(length = 50, nullable = false)
    private String plan = "free"; // free/premium/enterprise/lifetime

    // OAuth fields
    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    // Additional fields
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    // Helper methods for security
    public boolean isAccountNonExpired() {
        return !isDeleted;
    }

    public boolean isAccountNonLocked() {
        return !isDeleted;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return !isDeleted && isEmailVerified;
    }

    public GrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + role.name());
    }

    // Helper methods for usage tracking
    public void incrementApiUsage() {
        String currentMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        if (!currentMonth.equals(apiUsageMonth)) {
            apiUsageMonth = currentMonth;
            apiUsageCount = 0;
        }
        apiUsageCount++;
    }

    public void incrementUiCvCount() {
        String currentMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        if (!currentMonth.equals(uiCvMonth)) {
            uiCvMonth = currentMonth;
            uiCvCount = 0;
        }
        uiCvCount++;
    }

    public boolean canUseApi() {
        String currentMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        if (!currentMonth.equals(apiUsageMonth)) {
            return true; // New month, reset
        }
        return apiUsageCount < apiRateLimit;
    }

    public boolean canGenerateUiCv() {
        String currentMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        if (!currentMonth.equals(uiCvMonth)) {
            return true; // New month, reset
        }
        return uiCvCount < uiCvLimit;
    }
}
