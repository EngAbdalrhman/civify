package com.app.civify.entity.logs;

import com.app.civify.entity.User;
import com.app.civify.entity.enums.CvOperationAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Dedicated log entity for CV operations (upload, parse, tailor, mask).
 * Stores extracted PII as JSON for analytics and audit purposes.
 * Separate from general AuditLog to avoid interference.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cv_operation_logs", indexes = {
    @Index(name = "idx_cv_op_log_action", columnList = "action"),
    @Index(name = "idx_cv_op_log_created_at", columnList = "created_at")
})
public class CvOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User performing the operation. Nullable for guest users.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Type of CV operation performed
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CvOperationAction action;

    /**
     * Source of the request: INTERNAL (UI) or EXTERNAL_API
     */
    @Column(nullable = false, length = 20)
    private String source;

    /**
     * Endpoint path that was called
     */
    @Column(length = 255)
    private String endpoint;

    /**
     * Request ID for tracing
     */
    @Column(name = "request_id", length = 36)
    private String requestId;

    /**
     * Extracted PII data stored as JSON.
     * Contains: name, email, phone, address, linkedin, github, website, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_pii", columnDefinition = "jsonb")
    private Map<String, Object> extractedPii;

    /**
     * Processing time in milliseconds
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * Whether the operation was successful
     */
    @Column(nullable = false)
    private Boolean success;

    /**
     * Error message if operation failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Client IP address
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Client user agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Additional request metadata as JSON (filename, language, options, etc.)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_metadata", columnDefinition = "jsonb")
    private Map<String, Object> requestMetadata;

    /**
     * Timestamp of the operation
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
