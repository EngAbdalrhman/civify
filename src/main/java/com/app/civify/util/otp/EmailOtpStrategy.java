package com.app.civify.util.otp;

import com.app.civify.entity.User;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Email OTP strategy implementation with modern HTML template
 * @since 1.0.0
 * @version 2.0.0
 * @date 2025-12-09
 */
@Service
@RequiredArgsConstructor
public class EmailOtpStrategy implements OtpNotificationStrategy {

    private final EmailService emailService;

    @Override
    public void sendOtp(User user, String code) {
        String subject = "Civify - Email Verification Code";
        String body = String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background-color: white; padding: 30px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .otp-box { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; font-size: 36px; font-weight: bold; letter-spacing: 10px; text-align: center; padding: 25px; margin: 25px 0; border-radius: 8px; font-family: 'Courier New', monospace; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); }
                        .info-box { background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
                        ul { padding-left: 20px; margin: 10px 0; }
                        li { margin: 8px 0; }
                        .highlight { color: #667eea; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0; font-size: 28px;">✉️ Email Verification</h1>
                            <p style="margin: 10px 0 0 0; opacity: 0.9;">Civify Account Security</p>
                        </div>
                        
                        <div class="content">
                            <p>Hello <strong>%s</strong>,</p>
                            
                            <p>Thank you for choosing Civify! To complete your verification, please use the code below:</p>
                            
                            <div class="otp-box">%s</div>
                            
                            <div class="info-box">
                                <p style="margin: 0;"><strong>ℹ️ How to use this code:</strong></p>
                                <ul>
                                    <li>Enter this code in the verification page</li>
                                    <li>Complete your account setup</li>
                                    <li>Start using Civify!</li>
                                </ul>
                            </div>
                            
                            <div class="warning-box">
                                <p style="margin: 0;"><strong>⚠️ Important Security Information:</strong></p>
                                <ul>
                                    <li><span class="highlight">Valid for 30 minutes only</span></li>
                                    <li>Maximum 3 verification attempts within 15 minutes</li>
                                    <li>Never share this code with anyone</li>
                                    <li>Civify staff will never ask for this code</li>
                                </ul>
                            </div>
                            
                            <p style="margin-top: 25px;"><strong>Didn't request this code?</strong></p>
                            <p>If you didn't sign up for Civify, please ignore this email. Your information is safe.</p>
                            
                            <p style="margin-top: 25px; color: #666;">For security reasons, this verification code will expire in <strong>30 minutes</strong>.</p>
                        </div>
                        
                        <div class="footer">
                            <p style="margin: 5px 0;">This is an automated message from Civify</p>
                            <p style="margin: 5px 0;">© 2025 Civify. All rights reserved.</p>
                            <p style="margin: 5px 0; color: #999;">Please do not reply to this email</p>
                        </div>
                    </div>
                </body>
                </html>
                """, user.getUsername(), code);

        emailService.sendHtmlEmail(user.getEmail(), subject, body);
    }
}
