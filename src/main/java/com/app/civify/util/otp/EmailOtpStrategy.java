package com.app.civify.util.otp;

import com.app.civify.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Email OTP strategy implementation
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-01-25
 */
@Service
public class EmailOtpStrategy implements OtpNotificationStrategy {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendOtp(User user, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Civify - OTP Verification");
        message.setText("Your OTP verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");
        mailSender.send(message);
    }
}
