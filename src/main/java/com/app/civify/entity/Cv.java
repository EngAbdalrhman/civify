package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "cvs")
@EqualsAndHashCode(callSuper = true)
public class Cv extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String fileId; // Storage bucket ID

    @Column(length = 50000, columnDefinition = "TEXT")
    private String rawText;

    @Column(length = 50000, columnDefinition = "TEXT")
    private String structuredData; // JSON

    @Column(length = 50000, columnDefinition = "TEXT")
    private String profileData; // JSON
}
