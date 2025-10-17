package com.app.civify.service.auth;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.util.otp.OtpNotificationFactory;
import com.app.civify.util.otp.OtpNotificationStrategy;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Enhanced OTP service with Redis storage and strategy pattern
 * notifications
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final OtpNotificationFactory notificationFactory;
    private final Random random = new Random();

    // Rate limiting: max 3 OTP attempts per 15 minutes
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);



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
        redis.opsForValue().set(otpKey, code, OTP_EXPIRY);

        // Increment attempt counter
        redis.opsForValue().increment(attemptKey);
        redis.expire(attemptKey, RATE_LIMIT_WINDOW);

        // Send OTP via appropriate strategy
        OtpNotificationStrategy strategy = notificationFactory.getStrategy("EMAIL");
        strategy.sendOtp(user, code);
    }

    public boolean verify(String username, String code) {
        String otpKey = getOtpKey(username);
        String stored = redis.opsForValue().get(otpKey);

        if (stored != null && stored.equals(code)) {
            redis.delete(otpKey);
            redis.delete(getAttemptKey(username));
            return true;
        }
        return false;
    }

    private boolean isRateLimited(String username) {
        String attemptKey = getAttemptKey(username);
        String attempts = redis.opsForValue().get(attemptKey);
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
        // Redis TTL handles expiry automatically, but we can add cleanup logic here if needed
    }
}
