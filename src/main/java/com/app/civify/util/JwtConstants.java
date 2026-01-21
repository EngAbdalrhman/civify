package com.app.civify.util;

/**
 * JWT Constants for token generation and expiration
 */
public class JwtConstants {

    private JwtConstants() {
        // Private constructor to prevent instantiation
    }

    // Access token expiration: 15 minutes (in minutes)
    public static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 1;

    // Refresh token expiration: 30 days (in minutes)
    public static final long REFRESH_TOKEN_EXPIRATION_MINUTES = 30 * 24 * 60;

    // JWT token generation endpoint
    public static final String GENERATE_TOKEN_ENDPOINT = "/login";
    public static final String GENERATE_REFRESH_TOKEN_ENDPOINT = "/auth/refresh";

    // Refresh token cookie settings
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    public static final String TOKEN_COOKIE_PATH = "/"; // Allow cookie for all paths
    public static final int ACCESS_TOKEN_COOKIE_MAX_AGE = Math.toIntExact(ACCESS_TOKEN_EXPIRATION_MINUTES * 60);
    public static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 days in seconds
}
