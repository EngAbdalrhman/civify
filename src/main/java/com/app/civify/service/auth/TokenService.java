package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final StringRedisTemplate redis;

    @Value("${jwt.refresh-token-expiration:2592000000}") // 30 days default
    private long refreshTokenExpiration;

    @Value("${jwt.access-token-expiration:900000}") // 15 minutes default
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
        String refreshToken = generateRefreshToken(user.getUsername());

        // Track this refresh token for the user (for revoke all functionality)
        trackUserToken(user.getUsername(), refreshToken);

        log.debug("Created auth token pair for user: {}", user.getUsername());
        return new AuthTokenPair(accessToken, refreshToken);
    }

    /**
     * Generate new UUID refresh token and store in Redis Uses cryptographically
     * secure random UUID
     */
    private String generateRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        long daysToExpire = refreshTokenExpiration / (24 * 60 * 60 * 1000);
        redis.opsForValue().set(getRefreshKey(token), username, Duration.ofDays(daysToExpire));
        log.debug("Generated refresh token for user: {}, expires in {} days", username, daysToExpire);
        return token;
    }

    /**
     * Refresh access token using refresh token Implements refresh token
     * rotation for security (old token is deleted)
     *
     * @throws BadRequestException if refresh token is invalid or expired
     */
    public AuthTokenPair refreshAccessToken(String refreshToken, User user) {
        String key = getRefreshKey(refreshToken);
        String username = redis.opsForValue().get(key);

        if (username == null) {
            log.warn("Refresh token not found or expired for user: {}", user.getUsername());
            throw new BadRequestException("Invalid or expired refresh token");
        }

        if (!username.equals(user.getUsername())) {
            log.error("Refresh token mismatch! Token belongs to: {}, but user is: {}", username, user.getUsername());
            throw new BadRequestException("Refresh token does not belong to user");
        }

        // Rotate refresh token (delete old, issue new) - prevents token reuse attacks
        redis.delete(key);
        untrackUserToken(user.getUsername(), refreshToken);

        log.info("Refreshed access token for user: {}", username);

        // Generate new token pair
        return createAuthTokens(user);
    }

    /**
     * Revoke refresh token (logout from single device)
     */
    public void revokeRefreshToken(String refreshToken) {
        String key = getRefreshKey(refreshToken);
        String username = redis.opsForValue().get(key);

        redis.delete(key);

        if (username != null) {
            untrackUserToken(username, refreshToken);
            log.info("Revoked refresh token for user: {}", username);
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices) Useful for
     * security incidents or password changes
     */
    public void revokeAllUserTokens(String username) {
        String userTokensKey = getUserTokensKey(username);
        Set<String> tokens = redis.opsForSet().members(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redis.delete(getRefreshKey(token));
            }
            redis.delete(userTokensKey);
            log.warn("Revoked ALL tokens for user: {} (count: {})", username, tokens.size());
        }
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
                redis.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(ttlMillis));
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
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey));
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
        String userTokensKey = getUserTokensKey(username);
        redis.opsForSet().add(userTokensKey, refreshToken);
        // Set expiry on the set itself
        redis.expire(userTokensKey, Duration.ofMillis(refreshTokenExpiration));
    }

    /**
     * Untrack a refresh token for a user
     */
    private void untrackUserToken(String username, String refreshToken) {
        String userTokensKey = getUserTokensKey(username);
        redis.opsForSet().remove(userTokensKey, refreshToken);
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
