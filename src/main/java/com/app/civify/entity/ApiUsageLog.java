package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "api_usage_logs")
@EqualsAndHashCode(callSuper = true)
public class ApiUsageLog extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 10)
    private String month; // YYYY-MM format

    @Column(nullable = false)
    private Boolean success = true;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 50)
    private String ipAddress;
}
