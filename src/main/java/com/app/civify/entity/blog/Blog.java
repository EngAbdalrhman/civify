package com.app.civify.entity.blog;

import com.app.civify.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blogs_slug", columnList = "slug", unique = true),
        @Index(name = "idx_blogs_published", columnList = "published"),
        @Index(name = "idx_blogs_featured", columnList = "featured"),
        @Index(name = "idx_blogs_published_at", columnList = "publishedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, unique = true, length = 350)
    private String slug;

    @Lob
    @Column(nullable = false)
    private String content;

    // SEO fields
    @Column(length = 160)
    private String metaDescription;

    @Column(length = 500)
    private String metaKeywords;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(length = 100)
    private String readingTime; // e.g., "5 min read"

    // Publishing control
    @Builder.Default
    @Column(nullable = false)
    private Boolean published = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean featured = false;

    @Column
    private LocalDateTime publishedAt;

    // Timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Author relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "author_name", length = 100)
    private String authorName;
}
