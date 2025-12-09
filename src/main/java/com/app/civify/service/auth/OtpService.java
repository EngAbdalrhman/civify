package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cache.CacheService;
import com.app.civify.util.otp.OtpNotificationFactory;
import com.app.civify.util.otp.OtpNotificationStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Enhanced OTP service with Redis storage and strategy pattern
 * notifications. Now with automatic fallback to in-memory cache when Redis is unavailable.
 * @since 1.0.0
 * @version 1.1.0
 * @date 2025-01-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final CacheService cacheService;
    private final UserRepository userRepository;
    private final OtpNotificationFactory notificationFactory;
    private final Random random = new Random();

    // Rate limiting: max 3 OTP attempts per 15 minutes
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(30); // Increased for email delivery delays



    public void generateAndSend(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check rate limiting
        if (isRateLimited(username)) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String otpKey = getOtpKey(username);
        String attemptKey = getAttemptKey(username);

        // Store OTP with expiry
        cacheService.set(otpKey, code, OTP_EXPIRY);

        // Increment attempt counter
        cacheService.increment(attemptKey);
        cacheService.expire(attemptKey, RATE_LIMIT_WINDOW);

        // Send OTP via appropriate strategy
        OtpNotificationStrategy strategy = notificationFactory.getStrategy("EMAIL");
        strategy.sendOtp(user, code);
        
        log.info("OTP generated for user: {} using cache mode: {}", username, cacheService.getCacheMode());
    }

    /**
     * Generate OTP without sending email - for custom email templates
     * Returns the generated OTP code
     */
    public String generateOtpOnly(String username) {
        // Verify user exists (will throw if not found)
        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check rate limiting
        if (isRateLimited(username)) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String otpKey = getOtpKey(username);
        String attemptKey = getAttemptKey(username);

        // Store OTP with expiry
        cacheService.set(otpKey, code, OTP_EXPIRY);

        // Increment attempt counter
        cacheService.increment(attemptKey);
        cacheService.expire(attemptKey, RATE_LIMIT_WINDOW);

        log.info("OTP generated (no email sent) for user: {} using cache mode: {}", username, cacheService.getCacheMode());
        
        return code;
    }

    public boolean verify(String username, String code) {
        String otpKey = getOtpKey(username);
        String stored = cacheService.get(otpKey);

        if (stored != null && stored.equals(code)) {
            cacheService.delete(otpKey);
            cacheService.delete(getAttemptKey(username));
            log.info("OTP verified successfully for user: {}", username);
            return true;
        }
        return false;
    }

    private boolean isRateLimited(String username) {
        String attemptKey = getAttemptKey(username);
        String attempts = cacheService.get(attemptKey);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    private String getOtpKey(String username) {
        return "otp:" + username;
    }

    private String getAttemptKey(String username) {
        return "otp_attempts:" + username;
    }

    @Scheduled(fixedRate = 300000) // Runs every 5 minutes
    public void clearExpiredOtps() {
        // Cache service handles expiry automatically (Redis TTL or in-memory cleanup)
        log.debug("OTP cleanup check - cache mode: {}", cacheService.getCacheMode());
    }
}
