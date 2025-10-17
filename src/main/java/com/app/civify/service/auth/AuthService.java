package com.app.civify.service.auth;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;

public interface AuthService {

    AuthResponse register(AuthRequest req);

    AuthResponse login(AuthRequest req);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    void enable2fa(String username);

    AuthResponse verifyOtp(String username, String code);
}
