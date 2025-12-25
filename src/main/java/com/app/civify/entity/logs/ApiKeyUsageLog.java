package com.app.civify.entity.logs;

import com.app.civify.entity.security.ApiKey;
import com.app.civify.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key_usage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private Integer statusCode;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Helpers for Admin API
    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getApiKey() {
        if (apiKey != null) {
            return apiKey.getKeyPreview(); 
        }
        return null;
    }
}
