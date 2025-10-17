package com.app.civify.util.otp;

import com.app.civify.entity.User;
import org.springframework.stereotype.Service;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details SMS OTP strategy implementation
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
@Service
public class SmsOtpStrategy implements OtpNotificationStrategy {

    @Override
    public void sendOtp(User user, String otp) {
        // TODO: Implement SMS sending logic with provider like Twilio, AWS SNS, etc.
        // For now, just log the OTP (remove in production)
        System.out.println("SMS OTP for " + user.getUsername() + ": " + otp);
    }
}