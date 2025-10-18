package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for email verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final StringRedisTemplate redis;
    private final SubscriptionService subscriptionService;

    @Value("${email.verification.token-expiration:86400000}") // 24 hours default
    private long tokenExpiration;

    @Value("${email.verification.link:http://localhost:8077/apis/auth/verify-email}")
    private String verificationLinkBase;

    /**
     * Generate and send email verification
     */
    public void sendVerificationEmail(User user) {
        // Generate verification token
        String token = UUID.randomUUID().toString();
        
        // Store in Redis with 24h expiration
        long hoursToExpire = tokenExpiration / (60 * 60 * 1000);
        redis.opsForValue().set(getVerificationKey(token), user.getEmail(), Duration.ofHours(hoursToExpire));

        // Build verification link
        String verificationLink = verificationLinkBase + "?token=" + token;

        // Send email
        String subject = "Verify Your Email - Civify";
        String body = String.format(
                "Dear %s,\n\n"
                + "Thank you for registering with Civify!\n\n"
                + "Please verify your email address by clicking the link below:\n"
                + "%s\n\n"
                + "This link will expire in 24 hours.\n\n"
                + "If you didn't create this account, please ignore this email.\n\n"
                + "Best regards,\n"
                + "The Civify Team",
                user.getUsername(),
                verificationLink
        );

        emailService.sendEmail(user.getEmail(), subject, body);
        log.info("Verification email sent to {}", user.getEmail());
    }

    /**
     * Verify email with token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        String key = getVerificationKey(token);
        String email = redis.opsForValue().get(key);

        if (email == null) {
            log.warn("Invalid or expired verification token: {}", token);
            return false;
        }

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

        if (user.getIsEmailVerified()) {
            log.info("Email already verified for user: {}", user.getUsername());
            redis.delete(key); // Clean up token
            return true;
        }

        // Verify email
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // Create default FREE subscription now that email is verified
        subscriptionService.createDefaultFreeSubscription(user.getId());

        // Delete verification token
        redis.delete(key);

        log.info("Email verified successfully for user: {}", user.getUsername());
        return true;
    }

    /**
     * Resend verification email
     */
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    private String getVerificationKey(String token) {
        return "email:verification:" + token;
    }
}
