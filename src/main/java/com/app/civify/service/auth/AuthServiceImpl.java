package com.app.civify.service.auth;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.model.Role;
import com.app.civify.service.subscription.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refactored auth service with proper flows: - Register → Email verification
 * required - Login → Check email verification → Optional 2FA → Tokens - OAuth2
 * → Auto-verified → Tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final EmailVerificationService emailVerificationService;
    private final SubscriptionService subscriptionService;

    @Value("${email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    /**
     * Register new user - Sends verification email, does NOT return tokens
     */
    @Override
    @Transactional
    public AuthResponse register(AuthRequest req) {
        log.info("Registering new user: {}", req.getUsername());

        // Check if username already exists
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_USER);
        
        // Set verification status based on toggle
        // If verification is disabled, user is auto-verified
        user.setIsEmailVerified(!emailVerificationEnabled);
        
        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        if (emailVerificationEnabled) {
            // Send verification email
            emailVerificationService.sendVerificationEmail(user);
            log.info("User registered successfully: {}. Verification email sent.", req.getUsername());
            
            return AuthResponse.builder()
                    .message("Registration successful! Please check your email to verify your account.")
                    .requires2FA(false)
                    .build();
        } else {
            // If verification is disabled, create free subscription immediately
            subscriptionService.createDefaultFreeSubscription(user.getId());
            log.info("User registered successfully: {}. Auto-verified (dev mode).", req.getUsername());
            
            return AuthResponse.builder()
                    .message("Registration successful! You can now login.")
                    .requires2FA(false)
                    .build();
        }
    }


    /**
     * Login with username/password or email/password
     */
    @Override
    @Transactional
    public AuthResponse login(AuthRequest req) {
        // Validate that either username or email is provided, but not both
        if ((req.getUsername() == null || req.getUsername().trim().isEmpty())
                && (req.getEmail() == null || req.getEmail().trim().isEmpty())) {
            throw new BadRequestException("Either username or email must be provided");
        }

        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()
                && req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Provide either username OR email, not both");
        }

        // Determine login identifier (username or email)
        String loginIdentifier = req.getUsername() != null && !req.getUsername().trim().isEmpty()
                ? req.getUsername()
                : req.getEmail();

        log.info("Login attempt for user: {}", loginIdentifier);

        // Find user by username or email
        User user = req.getUsername() != null && !req.getUsername().trim().isEmpty()
                ? userRepository.findByUsername(req.getUsername())
                        .orElseThrow(() -> new BadRequestException("Invalid username or password"))
                : userRepository.findByEmail(req.getEmail())
                        .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Verify password manually since we need to handle both username and email login
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username/email or password");
        }

        // Check if email is verified (only if verification is enabled)
        if (emailVerificationEnabled && !user.getIsEmailVerified()) {
            log.warn("Login attempt with unverified email: {}", user.getUsername());
            throw new BadRequestException("Please verify your email before logging in. Check your inbox for verification link.");
        }

        // Update last login time
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Check if 2FA is enabled
        if (user.getTwoFactorEnabled()) {
            // Generate and send OTP
            otpService.generateAndSend(user.getUsername());
            log.info("2FA OTP sent to user: {}", user.getUsername());

            return AuthResponse.builder()
                    .message("OTP sent to your email. Please verify to complete login.")
                    .requires2FA(true)
                    .build();
        }

        // Generate tokens
        TokenService.AuthTokenPair tokens = tokenService.createAuthTokens(user);

        log.info("Login successful for user: {}", req.getUsername());

        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .requires2FA(false)
                .build();
    }

    /**
     * Verify 2FA OTP and return tokens
     */
    @Override
    @Transactional
    public AuthResponse verifyOtp(String username, String code) {
        log.info("Verifying OTP for user: {}", username);

        if (!otpService.verify(username, code)) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Update last login time
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        TokenService.AuthTokenPair tokens = tokenService.createAuthTokens(user);

        log.info("2FA verification successful for user: {}", username);

        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .requires2FA(false)
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        log.info("Refreshing access token");

        String username = tokenService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Generate new token pair (refresh token rotation)
        TokenService.AuthTokenPair tokens = tokenService.refreshAccessToken(refreshToken, user);

        log.info("Access token refreshed for user: {}", username);

        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .requires2FA(false)
                .build();
    }

    /**
     * Logout - Blacklist access token and revoke refresh token
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        log.info("Logging out user");

        // Blacklist access token for immediate invalidation
        if (accessToken != null && !accessToken.isEmpty()) {
            tokenService.blacklistAccessToken(accessToken);
        }

        // Revoke refresh token
        tokenService.revokeRefreshToken(refreshToken);
    }

    /**
     * Enable 2FA for user
     */
    @Override
    @Transactional
    public void enable2fa(String username) {
        log.info("Enabling 2FA for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        log.info("2FA enabled for user: {}", username);
    }

    /**
     * Disable 2FA for user
     */
    @Override
    @Transactional
    public void disable2fa(String username) {
        log.info("Disabling 2FA for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        log.info("2FA disabled for user: {}", username);
    }
}
