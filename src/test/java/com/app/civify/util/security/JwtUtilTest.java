package com.app.civify.util.security;

import com.app.civify.entity.User;
import com.app.civify.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtil, "secretKey",
                "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L); // 1 hour

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.ROLE_USER);
        testUser.setIsEmailVerified(true);
    }

    @Test
    void generateToken_WithUser_Success() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify claims
        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("ROLE_USER", claims.get("role", String.class));
        assertEquals(true, claims.get("emailVerified", Boolean.class));
    }

    @Test
    void generateToken_WithUsernameAndRole_Success() {
        // When
        String token = jwtUtil.generateToken("testuser", "ROLE_ADMIN");

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("role", String.class));
    }

    @Test
    void extractUsername_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void extractUserId_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Long userId = jwtUtil.extractUserId(token);

        // Then
        assertEquals(1L, userId);
    }

    @Test
    void extractEmail_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String email = jwtUtil.extractEmail(token);

        // Then
        assertEquals("test@example.com", email);
    }

    @Test
    void extractRole_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String role = jwtUtil.extractRole(token);

        // Then
        assertEquals("ROLE_USER", role);
    }

    @Test
    void extractEmailVerified_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Boolean emailVerified = jwtUtil.extractEmailVerified(token);

        // Then
        assertTrue(emailVerified);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean isValid = jwtUtil.validateToken(token, "testuser");

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WrongUsername_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean isValid = jwtUtil.validateToken(token, "wronguser");

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ExpiredToken_ThrowsException() {
        // Given - Create token with very short expiration
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 1L); // 1ms
        String token = jwtUtil.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When/Then
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateToken(token, "testuser"));
    }

    @Test
    void isTokenExpired_FreshToken_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean expired = jwtUtil.isTokenExpired(token);

        // Then
        assertFalse(expired);
    }

    @Test
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        // Given - Create token with very short expiration
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 1L);
        String token = jwtUtil.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When/Then
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenExpired(token));
    }

    @Test
    void extractAllClaims_ContainsAllCustomClaims() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Claims claims = jwtUtil.extractAllClaims(token);

        // Then
        assertNotNull(claims);
        assertTrue(claims.containsKey("userId"));
        assertTrue(claims.containsKey("email"));
        assertTrue(claims.containsKey("role"));
        assertTrue(claims.containsKey("emailVerified"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void generateToken_UnverifiedEmail_CreatesTokenWithFalseFlag() {
        // Given
        testUser.setIsEmailVerified(false);

        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        Boolean emailVerified = jwtUtil.extractEmailVerified(token);
        assertFalse(emailVerified);
    }

    @Test
    void generateToken_AdminRole_PreservesRole() {
        // Given
        testUser.setRole(Role.ROLE_ADMIN);

        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        String role = jwtUtil.extractRole(token);
        assertEquals("ROLE_ADMIN", role);
    }
}
