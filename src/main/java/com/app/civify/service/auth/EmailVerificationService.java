package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cache.CacheService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for email verification with automatic Redis fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CacheService cacheService;
    private final SubscriptionService subscriptionService;
    private final ResourceLoader resourceLoader;

    @Value("${email.verification.token-expiration:86400000}") // 24 hours default
    private long tokenExpiration;

    @Value("${app.backend.base-url:http://localhost:8077}")
    private String backendBaseUrl;

    /**
     * Generate and send email verification
     */
    public void sendVerificationEmail(User user) {
        // Generate verification token
        String token = UUID.randomUUID().toString();

        // Store in cache with 24h expiration
        long hoursToExpire = tokenExpiration / (60 * 60 * 1000);
        cacheService.set(getVerificationKey(token), user.getEmail(), Duration.ofHours(hoursToExpire));

        // Build verification link with dynamic backend URL
        String verificationLink = backendBaseUrl + "/apis/auth/verify-email?token=" + token;

        // Load and populate HTML template
        String htmlBody = loadEmailTemplate(user.getUsername(), verificationLink);

        // Send HTML email
        String subject = "Verify Your Email - Civify";
        emailService.sendHtmlEmail(user.getEmail(), subject, htmlBody);
        log.info("Verification email sent to {} using cache mode: {}", user.getEmail(), cacheService.getCacheMode());
    }

    /**
     * Load and populate HTML email template
     */
    private String loadEmailTemplate(String username, String verificationLink) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email-verification.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            // Replace placeholders
            return template
                    .replace("{{username}}", username)
                    .replace("{{verificationLink}}", verificationLink);
        } catch (IOException e) {
            log.error("Failed to load email template, falling back to plain text", e);
            // Fallback to plain text if template loading fails
            return String.format(
                    "Dear %s,<br><br>"
                    + "Thank you for registering with Civify!<br><br>"
                    + "Please verify your email address by clicking the link below:<br>"
                    + "<a href=\"%s\">%s</a><br><br>"
                    + "This link will expire in 24 hours.<br><br>"
                    + "If you didn't create this account, please ignore this email.<br><br>"
                    + "Best regards,<br>"
                    + "The Civify Team",
                    username,
                    verificationLink,
                    verificationLink
            );
        }
    }

    /**
     * Verify email with token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        String key = getVerificationKey(token);
        String email = cacheService.get(key);

        if (email == null) {
            log.warn("Invalid or expired verification token: {}", token);
            return false;
        }

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found for email: " + email));

        if (user.getIsEmailVerified()) {
            log.info("Email already verified for user: {}", user.getUsername());
            cacheService.delete(key); // Clean up token
            return true;
        }

        // Verify email
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // Create default FREE subscription now that email is verified
        subscriptionService.createDefaultFreeSubscription(user.getId());

        // Delete verification token
        cacheService.delete(key);

        log.info("Email verified successfully for user: {}", user.getUsername());
        return true;
    }

    /**
     * Resend verification email
     */
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    private String getVerificationKey(String token) {
        return "email:verification:" + token;
    }
}
