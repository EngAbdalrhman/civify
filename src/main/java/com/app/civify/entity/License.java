package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "licenses")
@EqualsAndHashCode(callSuper = true)
public class License extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String key;

    @Column(nullable = false, length = 255)
    private String createdBy;

    @Column(length = 255)
    private String activatedBy;

    @Column
    private LocalDateTime activatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 1000)
    private String notes;
}
