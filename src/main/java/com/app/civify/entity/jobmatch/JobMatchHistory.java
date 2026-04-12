package com.app.civify.entity.jobmatch;

import com.app.civify.entity.User;
import com.app.civify.entity.resume.Resume;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_match_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(length = 255)
    private String jobTitle;

    @Column(length = 255)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String jobDescription; // Store original JD to allow re-analysis

    private Integer overallScore;
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer keywordScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String matchResultJson; // Stores the full structured result (matched skills, gaps, suggestions)

    @Column(nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    public void prePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
