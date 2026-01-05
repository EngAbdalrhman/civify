package com.app.civify.service.auth;

import com.app.civify.dto.auth.PermissionsResponse;
import com.app.civify.entity.security.Group;
import com.app.civify.entity.security.Permission;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.util.EmailService;
import com.app.civify.service.cache.CacheService;
import com.app.civify.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for password recovery and permissions retrieval
 * 
 * Features:
 * - OTP-based password reset (reuses existing OtpService)
 * - Accepts email OR username for recovery
 * - Rate limiting via OtpService
 * - Token revocation on password change
 * - Encrypted permissions endpoint
 * 
 * @author Abdalrhman Mostafa
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PermissionsEncryptionService permissionsEncryptionService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final CacheService cacheService;

    /**
     * Initiate password reset - sends OTP to user's email
     * Accepts either email or username (user's choice)
     * 
     * @param email User's email (optional if username provided)
     * @param username User's username (optional if email provided)
     */
    @Transactional
    public void initiatePasswordReset(String email, String username) {
        // Validate that at least one identifier is provided
        if ((email == null || email.trim().isEmpty()) 
            && (username == null || username.trim().isEmpty())) {
            throw new BadRequestException("auth.password.reset.identifier.missing");
        }

        // Find user by email or username
        User user = null;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findByEmail(email).orElse(null);
        } else {
            user = userRepository.findByUsername(username).orElse(null);
        }

        // Security: Don't reveal if user exists or not
        if (user == null) {
            log.warn("Password reset requested for non-existent user: email={}, username={}", email, username);
            // Still return success to prevent user enumeration
            return;
        }

        // Generate OTP without sending generic email
        // We'll send a custom password recovery email instead
        try {
            String otp = otpService.generateOtpOnly(user.getUsername());
            
            // Send professional password recovery email with OTP
            sendPasswordRecoveryEmailWithOtp(user, otp);
            
            log.info("Password reset OTP sent to user: {}", user.getUsername());
        } catch (Exception e) {
            // Log error but don't expose it to prevent user enumeration
            log.error("Failed to send password reset OTP for user: {} - Error: {}", 
                    user.getUsername(), e.getMessage());
            log.warn("Email service may not be configured. Check SMTP settings in application.properties");
            // Still return success to prevent user enumeration
        }
    }

    /**
     * Step 1: Verify OTP and generate password reset token
     * Returns a temporary token valid for 5 minutes
     * 
     * @param email User's email (optional if username provided)
     * @param username User's username (optional if email provided)
     * @param otp OTP code from email
     * @return Temporary password reset token
     */
    @Transactional
    public String verifyOtpForPasswordReset(String email, String username, String otp) {
        // Validate inputs
        if ((email == null || email.trim().isEmpty()) 
            && (username == null || username.trim().isEmpty())) {
            throw new BadRequestException("auth.password.reset.identifier.missing");
        }

        if (otp == null || otp.trim().isEmpty()) {
            throw new BadRequestException("auth.password.reset.otp.required");
        }

        // Find user
        User user;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("auth.login.failed"));
        } else {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("auth.login.failed"));
        }

        // Verify OTP using existing OtpService
        if (!otpService.verify(user.getUsername(), otp)) {
            throw new BadRequestException("auth.otp.invalid");
        }

        // Generate temporary password reset token (valid for 5 minutes)
        String resetToken = generatePasswordResetToken(user.getUsername());
        
        log.info("OTP verified successfully for user: {}, reset token generated", user.getUsername());
        return resetToken;
    }

    /**
     * Step 2: Reset password using the temporary reset token
     * 
     * @param resetToken Temporary token from step 1
     * @param newPassword New password
     */
    @Transactional
    public void resetPasswordWithToken(String resetToken, String newPassword) {
        // Validate inputs
        if (resetToken == null || resetToken.trim().isEmpty()) {
            throw new BadRequestException("auth.password.reset.token.required");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("auth.password.weak.length");
        }

        // Verify and extract username from reset token
        String username = verifyPasswordResetToken(resetToken);
        if (username == null) {
            throw new BadRequestException("auth.password.reset.token.invalid");
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("user.notfound"));

        // Validate password strength
        validatePasswordStrength(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the reset token (one-time use)
        invalidatePasswordResetToken(resetToken);

        // Security: Revoke all refresh tokens to logout from all devices
        try {
            tokenService.revokeAllUserTokens(user.getUsername());
            log.info("Revoked all tokens for user after password reset: {}", user.getUsername());
        } catch (Exception e) {
            log.warn("Failed to revoke all tokens (may not be fully implemented yet): {}", e.getMessage());
        }

        log.info("Password reset successful for user: {}", username);
    }

    /**
     * Generate a temporary password reset token (valid for 5 minutes)
     */
    private String generatePasswordResetToken(String username) {
        String token = java.util.UUID.randomUUID().toString();
        String tokenKey = "password_reset_token:" + token;
        
        // Store username against token in cache with 5-minute expiry
        cacheService.set(tokenKey, username, java.time.Duration.ofMinutes(5));
        
        return token;
    }

    /**
     * Verify password reset token and return username
     */
    private String verifyPasswordResetToken(String token) {
        String tokenKey = "password_reset_token:" + token;
        return cacheService.get(tokenKey);
    }

    /**
     * Invalidate password reset token (one-time use)
     */
    private void invalidatePasswordResetToken(String token) {
        String tokenKey = "password_reset_token:" + token;
        cacheService.delete(tokenKey);
    }

    /**
     * Get user permissions (encrypted and plain)
     * 
     * @param token JWT access token
     * @return PermissionsResponse with permissions
     */
    public PermissionsResponse getUserPermissions(String token) {
        // Extract username from JWT
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new BadRequestException("Invalid token");
        }

        // Validate token
        if (!tokenService.validateAccessToken(token, username)) {
            throw new BadRequestException("Invalid or expired token");
        }

        // Get user with group and permissions
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getGroup() == null) {
            log.warn("User {} has no group assigned", username);
            return PermissionsResponse.builder()
                    .permissions(List.of())
                    .encryptedPermissions("")
                    .groupName("NONE")
                    .build();
        }

        // Fetch group with permissions eagerly
        Group group = groupRepository.findByIdWithPermissions(user.getGroup().getId())
                .orElseThrow(() -> new BadRequestException("Group not found"));

        List<Permission> permissions = group.getPermissions();

        // Extract permission names
        List<String> permissionNames = permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        // Encrypt permissions
        String encryptedPermissions = permissionsEncryptionService.encryptPermissions(permissions);

        return PermissionsResponse.builder()
                .permissions(permissionNames)
                .encryptedPermissions(encryptedPermissions)
                .groupName(group.getName())
                .build();
    }

    /**
     * Send professional password recovery email with OTP using centralized templates
     */
    private void sendPasswordRecoveryEmailWithOtp(User user, String otp) {
        try {
            String subject = "Password Reset Request - Verification Code";
            
            // Body content only - header/footer handled by EmailService based on user preference
            String bodyContent = String.format("""
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>We received a request to reset your Civify account password. Use the verification code below to complete the password reset process:</p>
                    
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; font-size: 32px; font-weight: bold; letter-spacing: 8px; text-align: center; padding: 20px; margin: 25px 0; border-radius: 8px; font-family: 'Courier New', monospace;">%s</div>
                    
                    <div style="background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>ℹ️ How to use this code:</strong></p>
                        <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                            <li style="margin: 8px 0;">Enter this code in the password reset page</li>
                            <li style="margin: 8px 0;">Choose your new secure password</li>
                            <li style="margin: 8px 0;">Confirm the password reset</li>
                        </ul>
                    </div>
                    
                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>⚠️ Security Information:</strong></p>
                        <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                            <li style="margin: 8px 0;"><strong>Valid for 30 minutes only</strong></li>
                            <li style="margin: 8px 0;">Maximum 3 reset attempts within 15 minutes</li>
                            <li style="margin: 8px 0;">All active sessions will be logged out after password reset</li>
                            <li style="margin: 8px 0;">Never share this code with anyone</li>
                        </ul>
                    </div>
                    
                    <p style="margin-top: 25px;"><strong>Didn't request this?</strong></p>
                    <p>If you didn't request a password reset, please ignore this email. Your account is secure and no changes have been made.</p>
                    
                    <p style="margin-top: 25px;">For security reasons, this verification code will expire in <strong>30 minutes</strong>.</p>
                    """, user.getUsername(), otp);

            emailService.sendHtmlEmailToUser(user.getId(), user.getEmail(), subject, bodyContent);
            log.info("Password recovery email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password recovery email to user: {}", user.getUsername(), e);
            throw e;
        }
    }

    /**
     * Validate password strength
     * Basic validation - can be enhanced with more rules
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new BadRequestException("auth.password.weak.length");
        }

        // Check for at least one letter and one number
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new BadRequestException("auth.password.weak.complexity");
        }

        // Prevent common weak passwords
        List<String> weakPasswords = List.of(
                "12345678", "password", "password123", "qwerty123",
                "admin123", "welcome123", "letmein123"
        );

        if (weakPasswords.contains(password.toLowerCase())) {
            throw new BadRequestException("auth.password.weak.common");
        }
    }
}
