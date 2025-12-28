package com.app.civify.service.auth;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.dto.auth.ChangePasswordRequest;

public interface AuthService {

    AuthResponse register(AuthRequest req);

    AuthResponse login(AuthRequest req);

    AuthResponse refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    void enable2fa(String username);

    void disable2fa(String username);

    AuthResponse verifyOtp(String username, String code);

    void changePassword(ChangePasswordRequest request);
}
