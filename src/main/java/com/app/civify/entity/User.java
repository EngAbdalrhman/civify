package com.app.civify.entity;

import com.app.civify.security.model.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

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

    // API Usage tracking (current usage counters)
    @Column(length = 255)
    private String apiKeyHash;

    @Column(length = 10, name = "api_usage_month")
    private String apiUsageMonth;

    @Column(name = "api_usage_count", nullable = false)
    private Integer apiUsageCount = 0;

    // Legacy CV tracking (deprecated but kept for compatibility)
    @Column(name = "cv_count", nullable = false)
    private Integer cvCount = 0;

    // UI CV tracking (current month usage counter)
    @Column(name = "ui_cv_count", nullable = false)
    private Integer uiCvCount = 0;

    @Column(length = 10, name = "ui_cv_month")
    private String uiCvMonth;

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

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSubscription> subscriptions;

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
        // Get API limit from active subscription, default to 500 if no subscription
        Integer apiLimit = getActiveSubscriptionApiLimit();
        return apiUsageCount < apiLimit;
    }

    public boolean canGenerateUiCv() {
        String currentMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        if (!currentMonth.equals(uiCvMonth)) {
            return true; // New month, reset
        }
        // Get CV limit from active subscription, default to 5 if no subscription
        Integer cvLimit = getActiveSubscriptionCvLimit();
        return uiCvCount < cvLimit;
    }

    /**
     * Get API rate limit from the most recent active subscription
     */
    private Integer getActiveSubscriptionApiLimit() {
        if (subscriptions != null && !subscriptions.isEmpty()) {
            return subscriptions.stream()
                    .filter(sub -> sub.isActive() && sub.getPlan() != null)
                    .max((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                    .map(sub -> sub.getPlan().getApiLimit())
                    .orElse(500); // Default free tier limit
        }
        return 500; // Default free tier limit
    }

    /**
     * Get CV limit from the most recent active subscription
     */
    private Integer getActiveSubscriptionCvLimit() {
        if (subscriptions != null && !subscriptions.isEmpty()) {
            return subscriptions.stream()
                    .filter(sub -> sub.isActive() && sub.getPlan() != null)
                    .max((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                    .map(sub -> sub.getPlan().getCvLimit())
                    .orElse(5); // Default free tier limit
        }
        return 5; // Default free tier limit
    }
}
