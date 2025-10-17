package com.app.civify.util.otp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Factory for OTP notification strategies
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
@Component
public class OtpNotificationFactory {

    @Autowired
    private Map<String, OtpNotificationStrategy> strategies;

    public OtpNotificationStrategy getStrategy(String type) {
        return type.equalsIgnoreCase("EMAIL")
                ? strategies.get("emailOtpStrategy")
                : strategies.get("smsOtpStrategy");
    }
}