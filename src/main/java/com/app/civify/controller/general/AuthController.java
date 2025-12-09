package com.app.civify.controller.general;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.dto.auth.PermissionsResponse;
import com.app.civify.service.auth.AuthService;
import com.app.civify.service.auth.EmailVerificationService;
import com.app.civify.service.auth.PasswordRecoveryService;
import com.app.civify.util.CommonResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Auth controller with complete flows: - Registration with email verification -
 * Login with optional 2FA - OAuth2 integration - Token refresh and logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordRecoveryService passwordRecoveryService;

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
    public ResponseEntity<CommonResponse> verifyEmail(@RequestParam("token") String token) {
        boolean verified = emailVerificationService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(CommonResponse.success(
                    "Email verified successfully! You can now login.",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(CommonResponse.error(
                    "Invalid or expired verification token"
            ));
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<CommonResponse> resendVerification(@RequestParam("email") String email) {
        log.info("Processing resend verification email request for email: {}", email);
        emailVerificationService.resendVerificationEmail(email);
        log.info("Verification email resent successfully for email: {}", email);
        return ResponseEntity.ok(CommonResponse.success(
                "Verification email sent! Please check your inbox.",
                null
        ));
    }

    /**
     * Verify 2FA OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verify2FA(
            @RequestParam("username") String username,
            @RequestParam("code") String code) {
        return ResponseEntity.ok(authService.verifyOtp(username, code));
    }

    /**
     * Refresh access token using refresh token
     */
    /**
     * Refresh access token using refresh token (from param or cookie)
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestParam(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "refreshToken", required = false) String refreshTokenCookie) {
        
        String tokenToUse = refreshToken;
        if (tokenToUse == null || tokenToUse.isEmpty()) {
            tokenToUse = refreshTokenCookie;
        }

        if (tokenToUse == null || tokenToUse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(authService.refresh(tokenToUse));
    }

    /**
     * Logout - blacklists access token and revokes refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("refreshToken") String refreshToken) {

        // Extract access token from Authorization header
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(CommonResponse.success("Logged out successfully", null));
    }

    /**
     * Enable 2FA for user
     */
    @PostMapping("/enable-2fa")
    public ResponseEntity<CommonResponse> enable2FA(@RequestParam("username") String username) {
        authService.enable2fa(username);
        return ResponseEntity.ok(CommonResponse.success("2FA enabled successfully", null));
    }

    /**
     * Disable 2FA for user
     */
    @PostMapping("/disable-2fa")
    public ResponseEntity<CommonResponse> disable2FA(@RequestParam("username") String username) {
        authService.disable2fa(username);
        return ResponseEntity.ok(CommonResponse.success("2FA disabled successfully", null));
    }

    /**
     * Get user permissions (encrypted and plain)
     * Requires valid JWT in Authorization header
     */
    @GetMapping("/permissions")
    public ResponseEntity<PermissionsResponse> getUserPermissions(
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract token from "Bearer <token>" header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        
        String token = authHeader.substring(7);
        PermissionsResponse response = passwordRecoveryService.getUserPermissions(token);
        
        log.info("Permissions retrieved for user from token");
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate password reset - sends OTP to user's email
     * Accepts either email or username
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<CommonResponse> forgotPassword(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username) {
        
        log.info("Processing forgot password request");
        passwordRecoveryService.initiatePasswordReset(email, username);
        
        return ResponseEntity.ok(CommonResponse.success(
                "If an account exists with this email/username, you will receive a password reset OTP shortly.",
                null
        ));
    }

    /**
     * Step 1: Verify OTP for password reset
     * Returns a temporary reset token valid for 5 minutes
     */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<CommonResponse> verifyResetOtp(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username,
            @RequestParam String otp) {
        
        log.info("Processing OTP verification for password reset");
        String resetToken = passwordRecoveryService.verifyOtpForPasswordReset(email, username, otp);
        
        return ResponseEntity.ok(CommonResponse.success(
                "OTP verified successfully. You may now reset your password.",
                Map.of("resetToken", resetToken)
        ));
    }

    /**
     * Step 2: Reset password using temporary reset token
     * The reset token is obtained from step 1 (verify OTP)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<CommonResponse> resetPassword(
            @RequestParam String resetToken,
            @RequestParam String newPassword) {
        
        log.info("Processing password reset with token");
        passwordRecoveryService.resetPasswordWithToken(resetToken, newPassword);
        
        return ResponseEntity.ok(CommonResponse.success(
                "Password reset successful! Please login with your new password.",
                null
        ));
    }
}
