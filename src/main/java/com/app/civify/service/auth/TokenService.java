package com.app.civify.service.auth;

import com.app.civify.util.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Unified token service combining JWT and refresh token management
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    public String generateAccessToken(String username, String role) {
        return jwtUtil.generateAccessToken(username, role);
    }

    public String generateRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(getRefreshKey(token), username, Duration.ofDays(14));
        return token;
    }

    public String verifyAndRotateRefreshToken(String refreshToken) {
        String key = getRefreshKey(refreshToken);
        String username = redis.opsForValue().get(key);

        if (username == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Rotate refresh token (delete old, issue new)
        redis.delete(key);
        return username;
    }

    public void revokeRefreshToken(String refreshToken) {
        redis.delete(getRefreshKey(refreshToken));
    }

    public boolean validateAccessToken(String token) {
        try {
            jwtUtil.extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsernameFromAccessToken(String token) {
        return jwtUtil.extractUsername(token);
    }

    public String extractRoleFromAccessToken(String token) {
        return jwtUtil.extractRole(token);
    }

    private String getRefreshKey(String token) {
        return "refresh:" + token;
    }
}
