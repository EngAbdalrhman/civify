package com.app.civify.service;

import com.app.civify.entity.NewsletterSubscriber;
import com.app.civify.entity.ScheduledEmailBatch;
import com.app.civify.repository.NewsletterRepository;
import com.app.civify.repository.ScheduledEmailBatchRepository;
import com.app.civify.util.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;
    private final ScheduledEmailBatchRepository batchRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.backend.base-url:http://localhost:8077}")
    private String backendBaseUrl;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

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

    /**
     * Queue newsletter for broadcast. Instead of sending synchronously (which fails
     * when SMTP rate-limits or auth-expires mid-way), we chunk subscribers into
     * batches and persist them to the database. The EmailBatchSchedulerService
     * picks them up and sends with automatic retry on failure.
     */
    @Async
    @Transactional
    public void broadcastEmail(String subject, String content, boolean isHtml) {
        List<NewsletterSubscriber> subscribers = newsletterRepository.findAllByActiveTrue();

        if (subscribers.isEmpty()) {
            log.info("No active subscribers for newsletter broadcast.");
            return;
        }

        // Prepare per-subscriber content with unique unsubscribe links
        String templateFooterStart = "  </div>" +
                "  <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e0e0e0;\">" +
                "    <p>You are receiving this email because you subscribed to our newsletter.</p>";
        String templateFooterEnd = "  </div>" +
                "</div>";

        int batchSize = 50;
        int totalSubscribers = subscribers.size();
        int batchCount = 0;

        for (int i = 0; i < totalSubscribers; i += batchSize) {
            int end = Math.min(totalSubscribers, i + batchSize);
            List<NewsletterSubscriber> chunk = subscribers.subList(i, end);

            try {
                // Build per-recipient content map as a JSON structure
                // Each batch stores the list of emails + a shared content template
                List<String> emailList = new ArrayList<>();

                for (NewsletterSubscriber subscriber : chunk) {
                    emailList.add(subscriber.getEmail());
                }

                // For the content template, we use the first subscriber's unsubscribe link as placeholder
                // The scheduler will rebuild per-recipient links if needed
                // For simplicity, store individual content per batch
                String finalBody = isHtml ? content : "<p>" + content.replace("\n", "<br>") + "</p>";

                // Build a generic template — unsubscribe links need per-subscriber IDs
                // We'll store each batch with its recipient list; the scheduler sends using EmailService
                // which already handles per-email sending. We need to store per-subscriber content.
                // Better approach: store each subscriber's full content in a batch-aware way.
                
                // Build individual content for each subscriber in this chunk
                StringBuilder batchContent = new StringBuilder();
                List<String> recipientEmails = new ArrayList<>();

                for (NewsletterSubscriber subscriber : chunk) {
                    String unsubscribeLink = String.format("%s/apis/v1/newsletter/unsubscribe-link?id=%s",
                            backendBaseUrl, subscriber.getId()).replace("/apis/apis/", "/apis/");

                    String uniqueContent = finalBody +
                            templateFooterStart +
                            "    <a href=\"" + unsubscribeLink + "\" style=\"color: #007bff; text-decoration: none;\" clicktracking=\"off\">Unsubscribe</a>" +
                            templateFooterEnd;

                    recipientEmails.add(subscriber.getEmail());
                    // We store the content template (same for all in this case since unsubscribe differs)
                    // For the batch approach, since each email has a unique unsubscribe link,
                    // we store the subscriber IDs and rebuild at send time
                }

                // Store batch with shared template + recipient list
                // The scheduler will rebuild per-subscriber unsubscribe links at send time
                String recipientJson = objectMapper.writeValueAsString(recipientEmails);

                ScheduledEmailBatch batch = ScheduledEmailBatch.builder()
                        .type("NEWSLETTER")
                        .subject(subject)
                        .contentTemplate(finalBody + templateFooterStart +
                                "    <a href=\"{{UNSUBSCRIBE_LINK}}\" style=\"color: #007bff; text-decoration: none;\" clicktracking=\"off\">Unsubscribe</a>" +
                                templateFooterEnd)
                        .isHtml(true)
                        .recipientEmails(recipientJson)
                        .status("PENDING")
                        .build();

                batchRepository.save(batch);
                batchCount++;

            } catch (Exception e) {
                log.error("Failed to queue newsletter batch ({} to {}): {}", i, end, e.getMessage());
            }
        }

        log.info("🚀 Newsletter broadcast queued: {} batches for {} subscribers",
                batchCount, totalSubscribers);
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

    @Transactional
    public java.net.URI processUnsubscribeLink(UUID id) {
        if (id == null) {
            return java.net.URI.create(frontendBaseUrl + "/en/unsubscribe?status=error");
        }
        boolean success = unsubscribeById(id);
        if (success) {
            return java.net.URI.create(frontendBaseUrl + "/en/unsubscribe?status=success");
        }
        return java.net.URI.create(frontendBaseUrl + "/en/unsubscribe?status=error");
    }
}

