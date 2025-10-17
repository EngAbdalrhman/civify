package com.app.civify.util.otp;

import com.app.civify.entity.User;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details OTP notification strategy interface
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
public interface OtpNotificationStrategy {

    void sendOtp(User user, String otp);
}
