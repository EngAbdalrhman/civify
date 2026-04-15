package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tracks email batches for reliable delivery with retry support.
 * Used by the newsletter system to queue batches and retry failed ones.
 */
@Entity
@Table(name = "scheduled_email_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmailBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // NEWSLETTER, NOTIFICATION, etc.

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contentTemplate; // The HTML body template

    @Column(nullable = false)
    private boolean isHtml;

    /**
     * JSON array of recipient emails for this batch, e.g. ["a@b.com","c@d.com"]
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String recipientEmails;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SENDING, SENT, FAILED

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column
    private LocalDateTime nextRetryAt;

    @Column
    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
