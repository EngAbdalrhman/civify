package com.app.civify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "api_keys")
@EqualsAndHashCode(callSuper = true)
public class ApiKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String keyHash;

    @Column(nullable = false, length = 50)
    private String keyPreview;

    @Column
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private Boolean isActive = true;
}
