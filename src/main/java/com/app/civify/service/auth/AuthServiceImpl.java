package com.app.civify.service.auth;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.security.model.Role;

import lombok.RequiredArgsConstructor;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final OtpService otpService;

    @Override
    public AuthResponse register(AuthRequest req) {
        // basic register stub
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);
        String refresh = tokenService.generateRefreshToken(user.getUsername());
        String access = tokenService.generateAccessToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(access, refresh, false);
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        if (user.isTwoFactorEnabled()) {
            otpService.generateAndSend(user.getUsername());
            return new AuthResponse(null, null, true);
        }
        String refresh = tokenService.generateRefreshToken(user.getUsername());
        String access = tokenService.generateAccessToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(access, refresh, false);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        String subject = tokenService.verifyAndRotateRefreshToken(refreshToken);
        User user = userRepository.findByUsername(subject).orElseThrow();
        String access = tokenService.generateAccessToken(subject, user.getRole().name());
        String newRefresh = tokenService.generateRefreshToken(subject);
        return new AuthResponse(access, newRefresh, false);
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    public void enable2fa(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Override
    public AuthResponse verifyOtp(String username, String code) {
        if (!otpService.verify(username, code)) {
            throw new RuntimeException("Invalid OTP");
        }
        User user = userRepository.findByUsername(username).orElseThrow();
        String refresh = tokenService.generateRefreshToken(username);
        String access = tokenService.generateAccessToken(username, user.getRole().name());
        return new AuthResponse(access, refresh, false);
    }
}
