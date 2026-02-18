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
        String templateFooterStart = "  </div>" +
                "  <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e0e0e0;\">" +
                "    <p>You are receiving this email because you subscribed to our newsletter.</p>";

        String templateFooterEnd = "  </div>" +
                "</div>";

        java.util.Map<String, String> emailContentMap = new java.util.HashMap<>();

        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                // Construct unsubscribe link
                String unsubscribeLink = String.format("%s/v1/newsletter/unsubscribe-link?id=%s", backendBaseUrl, subscriber.getId());

                String finalBody;
                if (isHtml) {
                    finalBody = content;
                } else {
                     // Text mode: Escape HTML (basic) and wrap in paragraphs for newlines
                     finalBody = "<p>" + content.replace("\n", "<br>") + "</p>";
                }

                String uniqueContent = finalBody +
                                  templateFooterStart +
                                  "    <a href=\"" + unsubscribeLink + "\" style=\"color: #007bff; text-decoration: none; clicktracking=\"off\" \">Unsubscribe</a>" +
                                  templateFooterEnd;

                emailContentMap.put(subscriber.getEmail(), uniqueContent);

            } catch (Exception e) {
                log.error("Failed to prepare newsletter for " + subscriber.getEmail() + ": " + e.getMessage());
            }
        }
        
        // Send all in batches using the EmailService
        if (!emailContentMap.isEmpty()) {
            log.info("🚀 Starting newsletter broadcast to {} subscribers...", emailContentMap.size());
            emailService.sendBatchCustomEmails(emailContentMap, subject);
            log.info("✅ Newsletter broadcast completed.");
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
