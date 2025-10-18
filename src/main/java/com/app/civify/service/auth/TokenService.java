package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Simplified token service
 * - JWT access tokens via JwtUtil
 * - UUID refresh tokens via Redis
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Value("${jwt.refresh-token-expiration:2592000000}") // 30 days default
    private long refreshTokenExpiration;

    /**
     * Create complete auth token pair (access + refresh)
     */
    public AuthTokenPair createAuthTokens(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = generateRefreshToken(user.getUsername());
        return new AuthTokenPair(accessToken, refreshToken);
    }

    /**
     * Generate new UUID refresh token and store in Redis
     */
    private String generateRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        long daysToExpire = refreshTokenExpiration / (24 * 60 * 60 * 1000);
        redis.opsForValue().set(getRefreshKey(token), username, Duration.ofDays(daysToExpire));
        return token;
    }

    /**
     * Refresh access token using refresh token
     * Also rotates the refresh token for security
     */
    public AuthTokenPair refreshAccessToken(String refreshToken, User user) {
        String key = getRefreshKey(refreshToken);
        String username = redis.opsForValue().get(key);

        if (username == null) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        if (!username.equals(user.getUsername())) {
            throw new RuntimeException("Refresh token does not belong to user");
        }

        // Rotate refresh token (delete old, issue new)
        redis.delete(key);

        // Generate new token pair
        return createAuthTokens(user);
    }

    /**
     * Revoke refresh token (logout)
     */
    public void revokeRefreshToken(String refreshToken) {
        redis.delete(getRefreshKey(refreshToken));
    }

    /**
     * Validate access token
     */
    public boolean validateAccessToken(String token, String username) {
        try {
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract username from access token
     */
    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    /**
     * Extract user ID from access token
     */
    public Long extractUserId(String token) {
        return jwtUtil.extractUserId(token);
    }

    /**
     * Extract role from access token
     */
    public String extractRole(String token) {
        return jwtUtil.extractRole(token);
    }

    private String getRefreshKey(String token) {
        return "refresh:" + token;
    }

    /**
     * Auth token pair (access + refresh)
     */
    @Data
    @AllArgsConstructor
    public static class AuthTokenPair {
        private String accessToken;
        private String refreshToken;
    }
}
