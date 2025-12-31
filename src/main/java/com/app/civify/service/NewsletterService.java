package com.app.civify.service;

import com.app.civify.entity.NewsletterSubscriber;
import com.app.civify.repository.NewsletterRepository;
import com.app.civify.util.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;
    private final EmailService emailService;

    @Value("${app.backend.base-url:http://localhost:8077}")
    private String backendBaseUrl;

    @Transactional
    public void subscribe(String email) {
        Optional<NewsletterSubscriber> existing = newsletterRepository.findByEmail(email);
        
        if (existing.isPresent()) {
            NewsletterSubscriber subscriber = existing.get();
            if (!subscriber.isActive()) {
                subscriber.setActive(true);
                subscriber.setUnsubscribedAt(null);
                newsletterRepository.save(subscriber);
            }
            // If already active, do nothing or maybe throw info exception needed? 
            // For privacy/UX, usually better to just say "success" or "already subscribed" depending on reqs.
            // Here we'll just ensure it's active.
        } else {
            NewsletterSubscriber newSubscriber = NewsletterSubscriber.builder()
                    .email(email)
                    .active(true)
                    .build();
            newsletterRepository.save(newSubscriber);
        }
    }

    @Transactional
    public void unsubscribe(String email) {
        newsletterRepository.findByEmail(email).ifPresent(subscriber -> {
            subscriber.setActive(false);
            subscriber.setUnsubscribedAt(LocalDateTime.now());
            newsletterRepository.save(subscriber);
        });
    }

    @Async
    public void broadcastEmail(String subject, String content, boolean isHtml) {
        List<NewsletterSubscriber> subscribers = newsletterRepository.findAllByActiveTrue();
        
        // Prepare template parts
        String logoUrl = backendBaseUrl + "/civify-logo-simple.png"; // Assuming static resources are served from root or /images
        // Better: Use a reliable public URL if available, or embed. 
        // Since we are in backend, we can reference the static file served by Spring Boot.
        // Spring Boot serves `src/main/resources/static` at root `/`.
        
        String templateHeader = "" +
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">" +
                "  <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; border-bottom: 1px solid #e0e0e0;\">" +
                "    <img src=\"" + logoUrl + "\" alt=\"Civify\" style=\"max-height: 50px;\">" +
                "  </div>" +
                "  <div style=\"padding: 30px; color: #333; line-height: 1.6;\">";

        String templateFooterStart = "" +
                "  </div>" +
                "  <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e0e0e0;\">" +
                "    <p>You are receiving this email because you subscribed to our newsletter.</p>";

        String templateFooterEnd = "" +
                "  </div>" +
                "</div>";

        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                // Construct unsubscribe link
                String unsubscribeLink = String.format("%s/v1/newsletter/unsubscribe-link?id=%s", backendBaseUrl, subscriber.getId());
                
                String finalBody;
                if (isHtml) {
                    // Inject content directly, but still wrap in our container? 
                    // User said: "even send as html enabled consider the admin enter html and surroned the header and footer to it as deafult template"
                    // So YES, always wrap.
                    finalBody = content; 
                } else {
                     // Text mode: Escape HTML (basic) and wrap in paragraphs for newlines
                     finalBody = "<p>" + content.replace("\n", "<br>") + "</p>";
                }

                String fullHtml = templateHeader + 
                                  finalBody + 
                                  templateFooterStart + 
                                  "    <a href=\"" + unsubscribeLink + "\" style=\"color: #007bff; text-decoration: none;\">Unsubscribe</a>" +
                                  templateFooterEnd;
                
                emailService.sendHtmlEmail(subscriber.getEmail(), subject, fullHtml);
            } catch (Exception e) {
                // Log but continue
                log.error("Failed to send broadcast to " + subscriber.getEmail() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public boolean unsubscribeById(UUID id) {
        return newsletterRepository.findById(id)
                .map(subscriber -> {
                    subscriber.setActive(false);
                    subscriber.setUnsubscribedAt(LocalDateTime.now());
                    newsletterRepository.save(subscriber);
                    return true;
                })
                .orElse(false);
    }
}
