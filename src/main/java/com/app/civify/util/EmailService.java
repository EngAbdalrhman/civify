package com.app.civify.util;

import com.app.civify.entity.enums.UserPreferredTheme;
import com.app.civify.service.user.UserPreferenceService;
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
 * Centralized Email Service
 * 
 * Provides consistent email template headers/footers based on user theme preferences.
 * All email sending should go through this service to ensure consistent branding.
 *
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserPreferenceService userPreferenceService;

    @Value("${default.email:${spring.mail.username}}")
    private String defaultFromEmail;

    /**
     * Send plain text email (no template wrapping)
     */
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * Send HTML email with default LIGHT theme header/footer
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML content of the email (body only, header/footer will be added)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendHtmlEmail(to, subject, htmlBody, UserPreferredTheme.LIGHT);
    }

    /**
     * Send HTML email with specified theme header/footer
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML content of the email (body only)
     * @param theme    theme for header/footer styling
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody, UserPreferredTheme theme) {
        String body = getTemplateHeader(theme) + htmlBody + getTemplateFooter();
        sendRawHtmlEmail(to, subject, body);
    }

    /**
     * Send HTML email to a user with their preferred theme (cached lookup)
     *
     * @param userId   user ID for theme preference lookup
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML content of the email (body only)
     */
    public void sendHtmlEmailToUser(Long userId, String to, String subject, String htmlBody) {
        UserPreferredTheme theme = userPreferenceService.getUserTheme(userId);
        sendHtmlEmail(to, subject, htmlBody, theme);
    }

    /**
     * Send raw HTML email without any template wrapping
     * Use this for emails that already have complete HTML structure
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param fullHtml complete HTML email content
     */
    public void sendRawHtmlEmail(String to, String subject, String fullHtml) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(fullHtml, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    // ============= Public Template Methods =============

    /**
     * Get template header based on theme preference
     *
     * @param theme the theme to use (LIGHT or DARK)
     * @return HTML header string
     */
    public String getTemplateHeader(UserPreferredTheme theme) {
        return theme == UserPreferredTheme.DARK ? getTemplateDarkHeader() : getTemplateLightHeader();
    }

    /**
     * Get standard template footer
     *
     * @return HTML footer string
     */
    public String getTemplateFooter() {
        return "  </div>" + // Closes content Body div
                "</div>" +   // Closes container div
                "<div style=\"text-align: center; font-size: 12px; color: #999; margin-top: 20px;\">" +
                "  <p>&copy; 2026 Civify. All rights reserved.</p>" +
                "</div>";
    }

    // ============= Private Template Methods =============

    private static @NotNull String getTemplateLightHeader() {
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

    // ============= Utility Methods =============

    private String dateFormat(String dateString) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        return dateTime.format(outputFormatter);
    }
}
