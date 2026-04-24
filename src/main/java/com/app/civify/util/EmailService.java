package com.app.civify.util;

import com.app.civify.entity.User;
import com.app.civify.entity.billing.Invoice;
import com.app.civify.entity.enums.UserPreferredTheme;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.service.user.UserPreferenceService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized Email Service
 * <p>
 * Provides consistent email template headers/footers based on user theme preferences.
 * All email sending should go through this service to ensure consistent branding.
 *
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
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

    /**
     * Send HTML emails to a list of users in batches.
     * Reuses the transport connection and introduces delays to avoid rate limits.
     *
     * @param users          list of recipients
     * @param subject        email subject
     * @param messageContent body content (will be wrapped in theme)
     */
    public void sendHtmlEmailsToUsers(java.util.List<User> users, String subject, String messageContent) {
        if (users == null || users.isEmpty()) {
            return;
        }

        int batchSize = 50;
        int totalUsers = users.size();

        for (int i = 0; i < totalUsers; i += batchSize) {
            int end = Math.min(totalUsers, i + batchSize);
            java.util.List<User> batch = users.subList(i, end);

            try {
                // Create a batch of MimeMessages
                MimeMessage[] messages = new MimeMessage[batch.size()];

                for (int j = 0; j < batch.size(); j++) {
                    User user = batch.get(j);
                    UserPreferredTheme theme = userPreferenceService.getUserTheme(user.getId());
                    String body = getTemplateHeader(theme) + messageContent + getTemplateFooter();

                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(defaultFromEmail);
                    helper.setTo(user.getEmail());
                    helper.setSubject(subject);
                    helper.setText(body, true);

                    messages[j] = message;
                }

                // Send the batch
                mailSender.send(messages);
                log.info("📧 Sent batch of {} emails ({} to {})", batch.size(), i + 1, end);

                // Delay to be respectful of rate limits (e.g., 1 second between batches)
                if (end < totalUsers) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (Exception e) {
                log.error("❌ Failed to send batch emails (users {} to {}): {}", i, end, e.getMessage());
                // Rethrow so the caller (e.g. scheduler) knows the chunk failed and can handle retries
                throw new RuntimeException("Failed to send batch emails", e);
            }
        }
    }

    /**
     * Send custom HTML emails to multiple recipients in batches.
     * Useful when each recipient needs unique content (e.g., unsubscribe links).
     *
     * @param recipientContentMap Map of "Email Address" -> "HTML Body Content"
     * @param subject             email subject
     */
    public void sendBatchCustomEmails(java.util.Map<String, String> recipientContentMap, String subject) {
        if (recipientContentMap == null || recipientContentMap.isEmpty()) {
            return;
        }

        int batchSize = 50;
        java.util.List<String> emails = new java.util.ArrayList<>(recipientContentMap.keySet());
        int totalEmails = emails.size();

        for (int i = 0; i < totalEmails; i += batchSize) {
            int end = Math.min(totalEmails, i + batchSize);
            java.util.List<String> batchEmails = emails.subList(i, end);

            try {
                MimeMessage[] messages = new MimeMessage[batchEmails.size()];

                for (int j = 0; j < batchEmails.size(); j++) {
                    String email = batchEmails.get(j);
                    String content = recipientContentMap.get(email);

                    // Apply default LIGHT theme wrapping (consistent with sendHtmlEmail default)
                    String fullBody = getTemplateHeader(UserPreferredTheme.LIGHT) + content + getTemplateFooter();

                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(defaultFromEmail);
                    helper.setTo(email);
                    helper.setSubject(subject);
                    helper.setText(fullBody, true);

                    messages[j] = message;
                }

                // Send batch
                mailSender.send(messages);
                log.info("📧 Sent newsletter batch of {} emails ({} to {})", batchEmails.size(), i + 1, end);

                // Delay
                if (end < totalEmails) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (Exception e) {
                log.error("❌ Failed to send newsletter batch ({} to {}): {}", i, end, e.getMessage());
                // Rethrow so the EmailBatchSchedulerService knows it failed and marks it for retry
                throw new RuntimeException("Failed to send custom email batch", e);
            }
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

    // ============= SaaS Email Templates =============

    /**
     * Send invoice payment confirmation email
     *
     * @param invoice the paid invoice
     */
    public void sendInvoicePaymentConfirmationEmail(Invoice invoice) {
        try {
            String subject = "Payment Confirmation - Invoice #" + invoice.getUuid().toString().substring(0, 8).toUpperCase();
            String bodyContent = String.format("""
                            <p>Dear <strong>%s</strong>,</p>
                            
                            <p>Thank you for your payment! Here's your receipt.</p>
                            
                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef;"><strong>Invoice #</strong></td>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef; text-align: right;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef;"><strong>Plan</strong></td>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef; text-align: right;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef;"><strong>Amount</strong></td>
                                        <td style="padding: 10px 0; border-bottom: 1px solid #e9ecef; text-align: right;">%s %s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 10px 0;"><strong>Date</strong></td>
                                        <td style="padding: 10px 0; text-align: right;">%s</td>
                                    </tr>
                                </table>
                            </div>
                            
                            <div style="background-color: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                <p style="margin: 0;"><strong>✓ Payment Successful</strong></p>
                                <p style="margin: 5px 0 0 0;">Your subscription is now active and ready to use.</p>
                            </div>
                            
                            <p>If you have any questions about your invoice, please contact our support team.</p>
                            
                            <p>Best regards,<br>Civify Team</p>
                            """,
                    invoice.getUserName(),
                    invoice.getUuid().toString().substring(0, 8).toUpperCase(),
                    invoice.getPlanName(),
                    invoice.getFinalAmount(),
                    invoice.getCurrency(),
                    invoice.getPaidAt() != null ? invoice.getPaidAt().toLocalDate() : "N/A"
            );

            sendHtmlEmailToUser(invoice.getUser().getId(), invoice.getUserEmail(), subject, bodyContent);
            log.info("Invoice payment confirmation email sent to {}", invoice.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send invoice payment confirmation email to {}: {}", invoice.getUserEmail(), e.getMessage());
        }
    }

    /**
     * Send subscription activation thank-you email
     *
     * @param user the user
     * @param plan the activated subscription plan
     */
    public void sendSubscriptionActivationEmail(User user, SubscriptionPlan plan, UserSubscription subscription, BigDecimal amountPaid) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            String startDateStr = subscription.getStartDate() != null ? subscription.getStartDate().format(formatter) : "N/A";
            String endDateStr = subscription.getEndDate() != null ? subscription.getEndDate().format(formatter) : "N/A";

            String subject = "Welcome to " + plan.getName() + " - Subscription Activated! 🎉";
            String bodyContent = String.format("""
                            <p>Dear <strong>%s</strong>,</p>
                            
                            <p>Great news! Your <strong>%s</strong> subscription has been successfully activated.</p>
                            
                            <div style="background-color: #e8f5e9; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;">
                                <h2 style="color: #2e7d32; margin: 0 0 10px 0;">🚀 You're All Set!</h2>
                                <p style="margin: 0; color: #388e3c;">Enjoy all the premium features of your new plan.</p>
                            </div>
                            
                            <h3 style="color: #333;">Transaction Details:</h3>
                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                <table style="width: 100%%; border-collapse: collapse; text-align: left;">
                                    <tr>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #e9ecef;"><strong>Amount Paid:</strong></td>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #e9ecef;">%s EGP</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #e9ecef;"><strong>Billing Cycle:</strong></td>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #e9ecef;">%s to %s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0;"><strong>Renewal Date:</strong></td>
                                        <td style="padding: 8px 0;">%s</td>
                                    </tr>
                                </table>
                            </div>
                            
                            <h3 style="color: #333;">What's Included:</h3>
                            <ul style="line-height: 1.8;">
                                <li>Up to <strong>%d</strong> CVs per month</li>
                                <li><strong>%d</strong> tokens for AI-powered features</li>
                                <li>Access to premium templates and features</li>
                                <li>Priority support</li>
                            </ul>
                            
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="https://civify.cv/app/dashboard" style="display: inline-block; padding: 14px 28px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600;">
                                    Go to Dashboard
                                </a>
                            </div>
                            
                            <p>Thank you for choosing Civify!</p>
                            
                            <p>Best regards,<br>Civify Team</p>
                            """,
                    user.getName(),
                    plan.getName(),
                    amountPaid.toString(),
                    startDateStr,
                    endDateStr,
                    endDateStr,
                    plan.getCvLimit() != null ? plan.getCvLimit() : 0,
                    plan.getTokenLimit() != null ? plan.getTokenLimit() : 0
            );

            sendHtmlEmailToUser(user.getId(), user.getEmail(), subject, bodyContent);
            log.info("Subscription activation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send subscription activation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Send welcome email to new users with getting started guide
     *
     * @param user the new user
     */
    public void sendWelcomeEmail(User user) {
        try {
            String subject = "Welcome to Civify! 🎉 Let's Get Started";
            String bodyContent = String.format("""
                            <p>Dear <strong>%s</strong>,</p>
                            
                            <p>Welcome to <strong>Civify</strong>! We're thrilled to have you join our community of professionals building standout CVs.</p>
                            
                            <div style="background-color: #e3f2fd; border-radius: 8px; padding: 25px; margin: 20px 0;">
                                <h2 style="color: #1565c0; margin: 0 0 15px 0;">🚀 Quick Start Guide</h2>
                            
                                <div style="margin-bottom: 20px;">
                                    <h4 style="margin: 0 0 8px 0; color: #1976d2;">1. Create Your First CV</h4>
                                    <p style="margin: 0; color: #555;">Head to your dashboard and click "New CV" to start building your professional resume.</p>
                                </div>
                            
                                <div style="margin-bottom: 20px;">
                                    <h4 style="margin: 0 0 8px 0; color: #1976d2;">2. Try AI-Powered Parsing</h4>
                                    <p style="margin: 0; color: #555;">Upload an existing CV and let our AI extract and organize your information automatically.</p>
                                </div>
                            
                                <div>
                                    <h4 style="margin: 0 0 8px 0; color: #1976d2;">3. Choose Your Template</h4>
                                    <p style="margin: 0; color: #555;">Browse our professional templates to find the perfect style for your industry.</p>
                                </div>
                            </div>
                            
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="https://civify.cv/app/dashboard" style="display: inline-block; padding: 14px 28px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600;">
                                    Start Creating Your CV
                                </a>
                            </div>
                            
                            <div style="background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                <p style="margin: 0;"><strong>💡 Pro Tip:</strong> Complete your profile to unlock personalized recommendations and improve your CV suggestions!</p>
                            </div>
                            
                            <p>If you have any questions, our support team is here to help.</p>
                            
                            <p>Best regards,<br>Civify Team</p>
                            """,
                    user.getName()
            );

            sendHtmlEmailToUser(user.getId(), user.getEmail(), subject, bodyContent);
            log.info("Welcome email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ============= Utility Methods =============

    private String dateFormat(String dateString) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        return dateTime.format(outputFormatter);
    }
}
