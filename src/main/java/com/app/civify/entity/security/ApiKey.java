package com.app.civify.entity.security;

import com.app.civify.entity.User;
import com.app.civify.entity.utils.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "api_keys")
@EqualsAndHashCode(callSuper = true)
public class ApiKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String keyHash;

    @Column(nullable = false, length = 50)
    private String keyPreview;

    @Column
    private LocalDateTime lastUsedAt;

    @Column(length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope", nullable = false, length = 50)
    private java.util.Set<String> scopes = new java.util.HashSet<>();

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasScope(String requiredScope) {
        if (scopes == null || scopes.isEmpty()) {
            return true; // Backward compatibility for legacy full-access keys
        }
        return scopes.contains(requiredScope) || scopes.contains("*");
    }
}
