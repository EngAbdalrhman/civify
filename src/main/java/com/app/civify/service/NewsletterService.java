package com.app.civify.service;

import com.app.civify.entity.NewsletterSubscriber;
import com.app.civify.repository.NewsletterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;

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
}
