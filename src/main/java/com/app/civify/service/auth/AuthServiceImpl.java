package com.app.civify.service.auth;

import com.app.civify.dto.auth.AuthRequest;
import com.app.civify.dto.auth.AuthResponse;
import com.app.civify.dto.auth.ChangePasswordRequest;
import com.app.civify.entity.enums.UserStatus;
import com.app.civify.entity.security.Group;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.bloomfilter.BloomFilterService;
import com.app.civify.service.i18n.MessageService;
import com.app.civify.service.referral.ReferralService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.NewsletterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final GroupRepository groupRepository;
    private final EmailVerificationService emailVerificationService;
    private final SubscriptionService subscriptionService;
    private final BloomFilterService bloomFilterService;
    private final MessageService messageService;
    private final DisposableEmailValidator disposableEmailValidator;
    private final ReferralService referralService;
    private final NewsletterService newsletterService;

    @Value("${email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${auth.2fa.enabled:false}")
    private boolean is2faGlobalEnabled;

    /**
     * Register new user - Sends verification email, does NOT return tokens
     */
    @Override
    @Transactional
    public AuthResponse register(AuthRequest req) {
        log.info("Registering new user: {}", req.getUsername());

        // Block disposable/temporary email addresses
        if (disposableEmailValidator.isDisposable(req.getEmail())) {
            log.warn("Registration blocked for disposable email: {}", req.getEmail());
            throw new BadRequestException("user.email.disposable");
        }

        if (bloomFilterService.mightHaveUsername(req.getUsername())) {
            // Only block if an ACTIVE (non-deleted) user exists with this username
            if (userRepository.findActiveByUsername(req.getUsername()).isPresent()) {
                throw new BadRequestException("user.username.exists");
            }
        }
        if (bloomFilterService.mightHaveEmail(req.getEmail())) {
            // Only block if an ACTIVE (non-deleted) user exists with this email
            if (userRepository.findActiveByEmail(req.getEmail()).isPresent()) {
                throw new BadRequestException("user.email.exists");
            }
        }

        Group defaultGroup = groupRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("Default group not found. Please run database migrations."));

        // Create new user
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setGroup(defaultGroup);

        
        // Set verification status based on toggle
        // If verification is disabled, user is auto-verified
        user.setIsEmailVerified(!emailVerificationEnabled);
        
        user.setTwoFactorEnabled(false);
        user.setReferralCode(referralService.generateUniqueCode());
        userRepository.save(user);
        newsletterService.subscribe(user.getEmail());

        if (req.getReferralCode() != null && !req.getReferralCode().trim().isEmpty()) {
            try {
                referralService.applyReferralCode(user, req.getReferralCode());
            } catch (Exception e) {
                log.error("Failed to apply referral code during registration: {}", e.getMessage());
            }
        }

        bloomFilterService.addUsername(req.getUsername());
        bloomFilterService.addEmail(req.getEmail());

        if (emailVerificationEnabled) {
            // Send verification email
            emailVerificationService.sendVerificationEmail(user);
            log.info("User registered successfully: {}. Verification email sent.", req.getUsername());
            
            return AuthResponse.builder()
                    .message(messageService.getMessage("auth.register.verification.sent"))
                    .requires2FA(false)
                    .build();
        } else {
            // If verification is disabled, create free subscription immediately
            subscriptionService.createDefaultFreeSubscription(user.getId());
            log.info("User registered successfully: {}. Auto-verified (dev mode).", req.getUsername());
            
            return AuthResponse.builder()
                    .message(messageService.getMessage("auth.register.success"))
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
            throw new BadRequestException("auth.login.identifier.missing");
        }

        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()
                && req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            throw new BadRequestException("auth.login.identifier.multiple");
        }

        // Determine login identifier (username or email)
        String loginIdentifier = req.getUsername() != null && !req.getUsername().trim().isEmpty()
                ? req.getUsername()
                : req.getEmail();

        log.info("Login attempt for user: {}", loginIdentifier);

        // Find user by username or email
        User user = req.getUsername() != null && !req.getUsername().trim().isEmpty()
                ? userRepository.findByUsername(req.getUsername())
                        .orElseThrow(() -> new BadRequestException("auth.login.failed"))
                : userRepository.findByEmail(req.getEmail())
                        .orElseThrow(() -> new BadRequestException("auth.login.failed"));

        // Verify password manually since we need to handle both username and email login
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("auth.login.failed");
        }

        // Check if user is banned/disabled
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("Login attempt for banned user: {}", user.getUsername());
            throw new BadRequestException("auth.login.banned");
        }

        // Check if email is verified (only if verification is enabled)
        if (emailVerificationEnabled && !user.getIsEmailVerified()) {
            log.warn("Login attempt with unverified email: {}", user.getUsername());
            throw new BadRequestException("auth.login.unverified");
        }

        // Update last login time
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Check if 2FA is enabled globally and for the user
        if (is2faGlobalEnabled && Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            // Generate and send OTP
            otpService.generateAndSend(user.getUsername());
            log.info("2FA OTP sent to user: {}", user.getUsername());

            return AuthResponse.builder()
                    .message(messageService.getMessage("auth.otp.sent"))
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
            throw new BadRequestException("auth.otp.invalid");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("user.notfound"));

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
                .orElseThrow(() -> new BadRequestException("user.notfound"));

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
                .orElseThrow(() -> new BadRequestException("user.notfound"));

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
                .orElseThrow(() -> new BadRequestException("user.notfound"));

        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        log.info("2FA disabled for user: {}", username);
    }

    /**
     * Change password for authenticated user
     * Verifies old password before updating to new password
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BadRequestException("auth.user.notauthenticated");
        }
        
        String username = auth.getName();
        log.info("Password change request for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("user.notfound"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("auth.password.current.incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);
    }
}
