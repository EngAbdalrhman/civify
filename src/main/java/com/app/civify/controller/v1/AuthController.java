package com.app.civify.controller.v1;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.service.auth.AuthService;
import com.app.civify.service.auth.EmailVerificationService;
import com.app.civify.util.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Auth controller with complete flows: - Registration with email verification -
 * Login with optional 2FA - OAuth2 integration - Token refresh and logout
 */
@RestController
@ApiV1Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Register new user - sends verification email
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        log.info("Processing user registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("User registration completed successfully for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email with token from email link
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam("token") String token) {
        boolean verified = emailVerificationService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Email verified successfully! You can now login.",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid or expired verification token"
            ));
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerification(@RequestParam("email") String email) {
        log.info("Processing resend verification email request for email: {}", email);
        emailVerificationService.resendVerificationEmail(email);
        log.info("Verification email resent successfully for email: {}", email);
        return ResponseEntity.ok(ApiResponse.success(
                "Verification email sent! Please check your inbox.",
                null
        ));
    }

    /**
     * Login with username/password
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Verify 2FA OTP
     */
    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verify2FA(
            @RequestParam("username") String username,
            @RequestParam("code") String code) {
        return ResponseEntity.ok(authService.verifyOtp(username, code));
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    /**
     * Logout - revokes refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestParam("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Enable 2FA for user
     */
    @PostMapping("/2fa/enable")
    public ResponseEntity<ApiResponse> enable2FA(@RequestParam("username") String username) {
        authService.enable2fa(username);
        return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", null));
    }

    /**
     * Disable 2FA for user
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse> disable2FA(@RequestParam("username") String username) {
        authService.disable2fa(username);
        return ResponseEntity.ok(ApiResponse.success("2FA disabled successfully", null));
    }
}
