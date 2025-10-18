package com.app.civify.entity;

import com.app.civify.entity.enums.CvGenerationType;
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

    // Generation tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CvGenerationType generationType = CvGenerationType.UI;

    // Job description used for tailoring (if applicable)
    @Column(length = 5000, columnDefinition = "TEXT")
    private String jobDescription;

    // Whether this CV has a cover letter generated
    @Column(nullable = false)
    private Boolean hasCoverLetter = false;

    // Whether this is a masked version (watermarked/anonymized)
    @Column(nullable = false)
    private Boolean isMasked = false;

    // Reference to subscription when CV was generated (for audit)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private UserSubscription subscription;
}
