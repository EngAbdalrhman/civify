package com.app.civify.service;

import com.app.civify.entity.NewsletterSubscriber;
import com.app.civify.entity.ScheduledEmailBatch;
import com.app.civify.repository.NewsletterRepository;
import com.app.civify.repository.ScheduledEmailBatchRepository;
import com.app.civify.util.EmailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scheduled service that processes queued email batches with retry logic.
 * Runs every 5 minutes and picks up PENDING or FAILED batches that are due.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailBatchSchedulerService {

    private final ScheduledEmailBatchRepository batchRepository;
    private final NewsletterRepository newsletterRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.backend.base-url:http://localhost:8077}")
    private String backendBaseUrl;

    /**
     * Process pending email batches every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000) // 5 min interval, 1 min initial delay
    @Transactional
    public void processEmailBatches() {
        List<ScheduledEmailBatch> batches = batchRepository.findBatchesReadyToSend(LocalDateTime.now());

        if (batches.isEmpty()) {
            return;
        }

        log.info("📬 Processing {} pending email batch(es)...", batches.size());

        for (ScheduledEmailBatch batch : batches) {
            processBatch(batch);
        }
    }

    private void processBatch(ScheduledEmailBatch batch) {
        try {
            batch.setStatus("SENDING");
            batchRepository.save(batch);

            List<String> recipients = objectMapper.readValue(
                    batch.getRecipientEmails(), new TypeReference<List<String>>() {});

            if (recipients.isEmpty()) {
                batch.setStatus("SENT");
                batch.setSentAt(LocalDateTime.now());
                batchRepository.save(batch);
                return;
            }

            // Build per-recipient content with resolved unsubscribe links
            Map<String, String> recipientContentMap = new HashMap<>();
            String template = batch.getContentTemplate();

            for (String email : recipients) {
                String content = template;

                // Resolve {{UNSUBSCRIBE_LINK}} placeholder if present
                if (content.contains("{{UNSUBSCRIBE_LINK}}")) {
                    Optional<NewsletterSubscriber> subscriber = newsletterRepository.findByEmail(email);
                    if (subscriber.isPresent()) {
                        String unsubscribeLink = String.format("%s/apis/v1/newsletter/unsubscribe-link?id=%s",
                                backendBaseUrl, subscriber.get().getId());
                        content = content.replace("{{UNSUBSCRIBE_LINK}}", unsubscribeLink);
                    } else {
                        // Subscriber not found (maybe unsubscribed), skip
                        content = content.replace("{{UNSUBSCRIBE_LINK}}", "#");
                    }
                }

                recipientContentMap.put(email, content);
            }

            // Delegate to existing batch send method
            emailService.sendBatchCustomEmails(recipientContentMap, batch.getSubject());

            batch.setStatus("SENT");
            batch.setSentAt(LocalDateTime.now());
            batch.setLastError(null);
            batchRepository.save(batch);

            log.info("✅ Email batch #{} sent successfully ({} recipients)", batch.getId(), recipients.size());

        } catch (Exception e) {
            log.error("❌ Email batch #{} failed (attempt {}): {}",
                    batch.getId(), batch.getRetryCount() + 1, e.getMessage());

            batch.setRetryCount(batch.getRetryCount() + 1);
            batch.setLastError(e.getMessage());

            if (batch.getRetryCount() >= batch.getMaxRetries()) {
                batch.setStatus("FAILED");
                log.warn("⛔ Email batch #{} exhausted all {} retries, marking as FAILED",
                        batch.getId(), batch.getMaxRetries());
            } else {
                batch.setStatus("FAILED");
                // Exponential backoff: 5min, 20min, 1hr
                long delayMinutes = (long) Math.pow(4, batch.getRetryCount()) * 5;
                batch.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                log.info("🔄 Email batch #{} scheduled for retry in {} minutes",
                        batch.getId(), delayMinutes);
            }

            batchRepository.save(batch);
        }
    }
}
