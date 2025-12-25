package com.app.civify.entity.resume;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.User;
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

    @Column(nullable = false)
    private String cvId; // Reference to CV document ID

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String tailoredCV;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(nullable = false)
    private String applicationStatus = "draft"; // draft, applied, interview, rejected, accepted

    @Column(columnDefinition = "TEXT")
    private String notes;
}
