package com.app.civify.entity;


import com.app.civify.annotation.FilterField;
import com.app.civify.annotation.FilterFieldMapping;
import com.app.civify.entity.enums.UserPreferredLanguage;
import com.app.civify.entity.enums.UserPreferredTheme;
import com.app.civify.entity.enums.UserStatus;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.security.Group;
import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.util.audit.UserAuditListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Builder
@EqualsAndHashCode(callSuper = true)
@EntityListeners(UserAuditListener.class)

@FilterFieldMapping({
        @FilterField(alias = "plan",   mapping = "subscriptions.plan.name"),
        @FilterField(alias = "status", mapping = "status"),
        @FilterField(alias = "oauthProvider",  mapping = "oauthProvider"),
        @FilterField(alias = "email",  mapping = "email")
})
public class User extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    // API Key for external integrations
    @Column(length = 255)
    private String apiKeyHash;

    // OAuth fields
    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    // Custom User Limits (overrides plan limits if set)
    @Column(name = "cv_limit")
    private Integer cvLimit;

    // Additional fields
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private Group group;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "extra_tokens", nullable = false)
    private Integer extraTokens = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // User Preferences
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 10)
    private UserPreferredLanguage preferredLanguage = UserPreferredLanguage.EN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_theme", nullable = false, length = 10)
    private UserPreferredTheme preferredTheme = UserPreferredTheme.LIGHT;

    @Builder.Default
    @Column(name = "welcome_email_sent", nullable = false)
    private Boolean welcomeEmailSent = false;

    // Referral System
    @Column(name = "referral_code", unique = true, length = 12)
    private String referralCode;

    @Column(name = "referred_by_code", length = 12)
    private String referredByCode;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "referral_count", nullable = false)
    private Integer referralCount = 0;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserSubscription> subscriptions;

    @Transient
    private String groupName;

    // Helper methods for security
    public boolean isAccountNonExpired() {
        return !isDeleted;
    }

    public boolean isAccountNonLocked() {
        return status != UserStatus.DISABLED;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        // Only check if account is deleted
        return !isDeleted && this.isAccountNonLocked();
    }

    public String getName() {
        if (firstName != null || lastName != null) {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }
        return username;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (group == null || group.getPermissions() == null) {
            return Collections.emptyList();
        }
        return group.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());
    }

    @PostLoad
    public void postLoad(){
        if (this.group != null)
            this.groupName = this.group.getName();
    }
}
