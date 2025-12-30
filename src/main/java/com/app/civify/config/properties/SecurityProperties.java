package com.app.civify.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Type-safe configuration for Security properties.
 * Aggregates JWT, Auth, and Email Verification settings.
 */
@Data
@Component
public class SecurityProperties {

    // JWT Properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Refresh Token Properties
    @Value("${refresh.token.secure}")
    private Boolean refreshTokenSecure;

    // Email Verification Properties
    @Value("${email.verification.enabled:true}")
    private Boolean emailVerificationEnabled;

    @Value("${email.verification.token-expiration:86400000}")
    private Long emailVerificationTokenExpiration;
}
