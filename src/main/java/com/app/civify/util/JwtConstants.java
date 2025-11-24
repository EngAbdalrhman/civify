package com.app.civify.util;

/**
 * JWT Constants for token generation and expiration
 */
public class JwtConstants {

    private JwtConstants() {
        // Private constructor to prevent instantiation
    }

    // Access token expiration: 15 minutes (in minutes)
    public static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 15;

    // Refresh token expiration: 7 days (in minutes)
    public static final long REFRESH_TOKEN_EXPIRATION_MINUTES = 7 * 24 * 60;

    // JWT token generation endpoint
    public static final String GENERATE_TOKEN_ENDPOINT = "/generate-token";

    // Refresh token cookie settings
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String REFRESH_TOKEN_COOKIE_PATH = "/refresh-token";
    public static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds
}
