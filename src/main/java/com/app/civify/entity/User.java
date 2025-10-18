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

    // API Key for external integrations
    @Column(length = 255)
    private String apiKeyHash;

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
}
