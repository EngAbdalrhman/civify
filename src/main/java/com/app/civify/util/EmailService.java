package com.app.civify.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @version 1.0.0
 * @details Email service
 * @date 2025-01-25
 * @since 1.0.0
 *
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${default.email:${spring.mail.username}}")
    private String defaultFromEmail;

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * Send HTML email
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML content of the email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        String body = getTemplateHeader() + htmlBody + getTemplateFooter();
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    // Helper Methods
    private String dateFormat(String dateString) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

        return dateTime.format(outputFormatter);
    }

    private static @NotNull String getTemplateHeader() {
        String logoUrl = "https://ik.imagekit.io/civify/civfiy%20logo.png";
        String brandColor = "#007bff"; // Civify Blue

        return new StringBuilder()
                // Main Container: Centered, nice shadow, rounded corners, white background
                .append("<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden;\">")

                // Logo Section: White background with a colored top border strip
                .append("  <div style=\"background-color: #ffffff; padding: 30px 20px; text-align: center; border-top: 4px solid ").append(brandColor).append(";\">")
                .append("    <img src=\"").append(logoUrl).append("\" alt=\"Civify\" style=\"max-height: auto; width: 130px; border: 0;\">")
                .append("  </div>")

                // Content Body Start: Better padding and typography settings
                .append("  <div style=\"padding: 40px 40px 20px 40px; color: #333333; line-height: 1.6; font-size: 16px;\">")
                .toString();
    }

    private static @NotNull String getTemplateDarkHeader() {
        String logoUrl = "https://ik.imagekit.io/civify/civfiy-logo-dark.png";

        return new StringBuilder()
                // Main Container
                .append("<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eeeeee; border-radius: 6px; overflow: hidden;\">")

                // Dark Header Background (Navy/Dark Gray)
                .append("  <div style=\"background-color: #1a202c; padding: 25px; text-align: center;\">")
                .append("    <img src=\"").append(logoUrl).append("\" alt=\"Civify\" style=\"max-height: auto; width: 130px\">")
                .append("  </div>")

                // Content Body Start
                .append("  <div style=\"padding: 30px; color: #2d3748; line-height: 1.6;\">")
                .toString();
    }

    private static @NotNull String getTemplateMinimalHeader() {
        String logoUrl = "https://ik.imagekit.io/civify/civify%20logo%20simple.png";

        return new StringBuilder()
                // Main Container: No border, just centering
                .append("<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto;\">")

                // Logo Section: Minimal padding
                .append("  <div style=\"padding: 40px 0 20px 0; text-align: center;\">")
                .append("    <img src=\"").append(logoUrl).append("\" alt=\"Civify\" style=\"max-height: 40px;\">")
                .append("  </div>")

                // Content Body Start
                .append("  <div style=\"padding: 0 10px; color: #111; line-height: 1.5; font-size: 16px;\">")
                .toString();
    }

    private static String getTemplateFooter() {
        return "  </div>" + // Closes content div
                "</div>" +   // Closes container div
                // Optional: Generic footer for system emails
                "<div style=\"text-align: center; font-size: 12px; color: #999; margin-top: 20px;\">" +
                "  <p>&copy; 2026 Civify</p>" +
                "</div>";
    }
}
