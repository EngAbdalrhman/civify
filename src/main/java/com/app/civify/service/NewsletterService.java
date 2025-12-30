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
    public void broadcastEmail(String subject, String content) {
        List<NewsletterSubscriber> subscribers = newsletterRepository.findAllByActiveTrue();
        
        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                // Construct unsubscribe link
                String unsubscribeLink = String.format("%s/api/v1/newsletter/unsubscribe-link?id=%s", backendBaseUrl, subscriber.getId());
                
                // Append unsubscribe footer to content
                String htmlBody = content + 
                        "<br><br><hr>" +
                        "<p style=\"font-size: 12px; color: #666;\">" +
                        "You are receiving this email because you subscribed to our newsletter. " +
                        "<a href=\"" + unsubscribeLink + "\">Unsubscribe</a>" +
                        "</p>";
                
                emailService.sendHtmlEmail(subscriber.getEmail(), subject, htmlBody);
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
