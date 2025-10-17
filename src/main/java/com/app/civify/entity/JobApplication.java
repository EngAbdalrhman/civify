package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "job_applications")
@EqualsAndHashCode(callSuper = true)
public class JobApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String cvId; // Reference to CV document ID

    @Column(nullable = false, length = 255)
    private String jobTitle;

    @Column(nullable = false, length = 255)
    private String company;

    @Column(length = 5000, columnDefinition = "TEXT")
    private String jobDescription;

    @Column(length = 5000, columnDefinition = "TEXT")
    private String coverLetter;

    @Column(nullable = false, length = 50)
    private String applicationStatus = "pending";
}
