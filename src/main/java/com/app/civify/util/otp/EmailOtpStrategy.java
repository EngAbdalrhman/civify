package com.app.civify.util.otp;

import com.app.civify.entity.User;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Email OTP strategy implementation using centralized email templates
 *
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @since 1.0.0
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
public class EmailOtpStrategy implements OtpNotificationStrategy {

    private final EmailService emailService;

    @Override
    public void sendOtp(User user, String code) {
        String subject = "Civify - Email Verification Code";
        
        String bodyContent = String.format("""
                <p>Hello <strong>%s</strong>,</p>
                
                <p>Thank you for choosing Civify! To complete your verification, please use the code below:</p>
                
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; font-size: 36px; font-weight: bold; letter-spacing: 10px; text-align: center; padding: 25px; margin: 25px 0; border-radius: 8px; font-family: 'Courier New', monospace; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);">%s</div>
                
                <div style="background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                    <p style="margin: 0;"><strong>ℹ️ How to use this code:</strong></p>
                    <ul style="padding-left: 20px; margin: 10px 0;">
                        <li style="margin: 8px 0;">Enter this code in the verification page</li>
                        <li style="margin: 8px 0;">Complete your account setup</li>
                        <li style="margin: 8px 0;">Start using Civify!</li>
                    </ul>
                </div>
                
                <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                    <p style="margin: 0;"><strong>⚠️ Important Security Information:</strong></p>
                    <ul style="padding-left: 20px; margin: 10px 0;">
                        <li style="margin: 8px 0;"><span style="color: #667eea; font-weight: bold;">Valid for 30 minutes only</span></li>
                        <li style="margin: 8px 0;">Maximum 3 verification attempts within 15 minutes</li>
                        <li style="margin: 8px 0;">Never share this code with anyone</li>
                        <li style="margin: 8px 0;">Civify staff will never ask for this code</li>
                    </ul>
                </div>
                
                <p style="margin-top: 25px;"><strong>Didn't request this code?</strong></p>
                <p>If you didn't sign up for Civify, please ignore this email. Your information is safe.</p>
                
                <p style="margin-top: 25px; color: #666;">For security reasons, this verification code will expire in <strong>30 minutes</strong>.</p>
                """, user.getUsername(), code);

        emailService.sendHtmlEmail(user.getEmail(), subject, bodyContent);
    }
}
