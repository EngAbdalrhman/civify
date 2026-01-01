package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    @Builder.Default
    boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime subscribedAt;
    
    private LocalDateTime unsubscribedAt;
}
