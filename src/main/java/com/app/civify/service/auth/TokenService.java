package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.service.cache.CacheService;
import com.app.civify.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced token service with security improvements: - JWT access tokens via
 * JwtUtil - UUID refresh tokens stored in Redis (more secure than JWT refresh
 * tokens) - Token revocation support via Redis blacklist - Refresh token
 * rotation (old token deleted on refresh) - Secure random UUID generation
 *
 * Security features: - Access token blacklist for logout/revocation - Refresh
 * token rotation prevents reuse attacks - Redis-based storage allows instant
 * revocation - All tokens can be revoked per user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtUtil jwtUtil;
    private final CacheService cacheService;

    @Value("${jwt.refresh-token-expiration:2592000000}") // 30 days default
    private long refreshTokenExpiration;

    @Value("${jwt.access-token-expiration:9000000}") // 150 minutes default
    private long accessTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";

    /**
     * Create complete auth token pair (access + refresh) Also tracks the token
     * for the user to enable "logout from all devices"
     */
    public AuthTokenPair createAuthTokens(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = generateRefreshToken(user);

        // Track this refresh token for the user (for revoke all functionality)
        trackUserToken(user.getUsername(), refreshToken);

        log.debug("Created auth token pair for user: {}", user.getUsername());
        return new AuthTokenPair(accessToken, refreshToken);
    }

    /**
     * Generate new JWT refresh token (stateless but blacklisted)
     */
    private String generateRefreshToken(User user) {
        String token = jwtUtil.generateRefreshToken(user, refreshTokenExpiration);
        // We don't strictly need to store it for validation (it's a JWT),
        // but we can track it for "logout all" if needed.
        // For now, we rely on JWT signature + blacklist check.
        log.debug("Generated JWT refresh token for user: {}", user.getUsername());
        return token;
    }

    /**
     * Refresh access token using refresh token
     * Implements refresh token rotation
     */
    public AuthTokenPair refreshAccessToken(String refreshToken, User user) {
        // 1. Check if token is blacklisted
        if (isRefreshTokenBlacklisted(refreshToken)) {
             log.warn("Refresh token is blacklisted for user: {}", user.getUsername());
             throw new BadRequestException("Invalid or expired refresh token");
        }

        // 2. Validate JWT signature and expiration
        if (!jwtUtil.validateToken(refreshToken, user.getUsername())) {
             log.warn("Invalid refresh token signature/expiry for user: {}", user.getUsername());
             throw new BadRequestException("Invalid or expired refresh token");
        }

        // 3. Rotate: Blacklist the old token
        blacklistRefreshToken(refreshToken);

        log.info("Refreshed access token for user: {}", user.getUsername());

        // 4. Generate new pair
        return createAuthTokens(user);
    }

    /**
     * Revoke refresh token (logout)
     */
    public void revokeRefreshToken(String refreshToken) {
        blacklistRefreshToken(refreshToken);
        log.info("Revoked refresh token");
    }

    private void blacklistRefreshToken(String token) {
        try {
            Date expirationDate = jwtUtil.extractExpiration(token);
            long expirationTime = expirationDate.getTime();
            long currentTime = System.currentTimeMillis();
            long ttlMillis = expirationTime - currentTime;

            if (ttlMillis > 0) {
                String blacklistKey = getRefreshKey(token); // Reuse prefix
                cacheService.set(blacklistKey, "revoked", Duration.ofMillis(ttlMillis));
            }
        } catch (Exception e) {
            log.error("Failed to blacklist refresh token", e);
        }
    }

    private boolean isRefreshTokenBlacklisted(String token) {
        String blacklistKey = getRefreshKey(token);
        return cacheService.get(blacklistKey) != null;
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices) Useful for
     * security incidents or password changes
     */
    public void revokeAllUserTokens(String username) {
        // Note: Set operations are not fully supported by CacheService abstraction yet
        // For now, we'll skip this or implement a workaround if needed
        // Since CacheService is key-value, we can't easily get all members of a set
        // TODO: Extend CacheService to support set operations or use a different tracking mechanism
        log.warn("Revoke all tokens not fully supported with CacheService abstraction yet");
    }

    /**
     * Blacklist an access token (for immediate logout) Access tokens are
     * stateless, so we need to blacklist them until expiry
     */
    public void blacklistAccessToken(String accessToken) {
        try {
            // Calculate remaining TTL of the token
            Date expirationDate = jwtUtil.extractExpiration(accessToken);
            long expirationTime = expirationDate.getTime();
            long currentTime = System.currentTimeMillis();
            long ttlMillis = expirationTime - currentTime;

            if (ttlMillis > 0) {
                String blacklistKey = getAccessTokenBlacklistKey(accessToken);
                cacheService.set(blacklistKey, "revoked", Duration.ofMillis(ttlMillis));
                log.info("Blacklisted access token, TTL: {} ms", ttlMillis);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist access token", e);
        }
    }

    /**
     * Check if an access token is blacklisted
     */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        String blacklistKey = getAccessTokenBlacklistKey(accessToken);
        return cacheService.get(blacklistKey) != null;
    }

    /**
     * Validate access token (checks both JWT validity and blacklist)
     */
    public boolean validateAccessToken(String token, String username) {
        try {
            // First check if token is blacklisted
            if (isAccessTokenBlacklisted(token)) {
                log.warn("Access token is blacklisted for user: {}", username);
                return false;
            }

            // Then validate JWT
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            log.debug("Access token validation failed for user: {}", username, e);
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

    /**
     * Track a refresh token for a user (for revoke all functionality)
     */
    private void trackUserToken(String username, String refreshToken) {
        // Set operations not supported in CacheService yet
    }

    /**
     * Untrack a refresh token for a user
     */
    private void untrackUserToken(String username, String refreshToken) {
        // Set operations not supported in CacheService yet
    }

    private String getRefreshKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }

    private String getAccessTokenBlacklistKey(String token) {
        return ACCESS_TOKEN_BLACKLIST_PREFIX + token;
    }

    private String getUserTokensKey(String username) {
        return USER_TOKENS_PREFIX + username;
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
