package com.app.civify.util;

import com.app.civify.config.properties.SecurityProperties;
import com.app.civify.entity.User;
import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Simplified JWT utility for token generation and validation Single
 * responsibility: Handle JWT access tokens only
 */
@Component
public class JwtUtil {

    private final SecurityProperties securityProperties;

    private final String secret;
    private final long accessTokenExpiration = JwtConstants.ACCESS_TOKEN_EXPIRATION_MINUTES * 60 * 1000;

    public JwtUtil(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.secret = securityProperties.getJwtSecret();
    }


    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT access token with all user claims Single method - no
     * overloading!
     */
    public String generateToken(User user) {
        // Extract group name safely, fallback to "USER" if group not assigned
        String groupName = user.getGroup() != null ? user.getGroup().getName() : "USER";
        
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("group", groupName) // Changed from "role" to "group" with actual group name
                .claim("emailVerified", user.getIsEmailVerified())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }



    /**
     * Generate token from username with specified expiry duration in minutes
     */
    public String generateToken(String username, long expiryMinutes) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMinutes * 60 * 1000)) // convert minutes to milliseconds
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate refresh token (long lived)
     */
    public String generateRefreshToken(User user, long expirationMillis) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate token against username
     */
    public boolean validateToken(String token, String username) {
        try {
            return username.equals(extractUsername(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract group from token
     */
    public String extractGroup(String token) {
        return extractClaim(token, claims -> claims.get("group", String.class));
    }



    /**
     * Extract email verified status from token
     */
    public Boolean extractEmailVerified(String token) {
        return extractClaim(token, claims -> claims.get("emailVerified", Boolean.class));
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate and extract username from token
     */
    public String validateAndExtractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null; // Invalid or expired JWT
        }
    }

    /**
     * Generate a signed JWT token containing user permissions
     * Used for secure permission transfer to frontend
     * 
     * @param username User's username
     * @param permissions List of permission names
     * @param groupName User's group name
     * @return Signed JWT token (24 hour expiry)
     */
    public String generatePermissionToken(String username, List<String> permissions, String groupName) {
        return Jwts.builder()
                .setSubject(username)
                .claim("permissions", permissions)
                .claim("groupName", groupName)
                .claim("type", "permission")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract permissions from a permission token
     * @param token Permission JWT token
     * @return List of permissions, or null if invalid
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissionsFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            if (!"permission".equals(type)) {
                return null;
            }
            return claims.get("permissions", List.class);
        } catch (Exception e) {
            return null;
        }
    }
}
