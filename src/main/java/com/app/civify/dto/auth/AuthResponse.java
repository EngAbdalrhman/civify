package com.app.civify.dto.auth;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private boolean requiresOtp;

    public AuthResponse(String accessToken, String refreshToken, boolean requiresOtp) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.requiresOtp = requiresOtp;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }
}
