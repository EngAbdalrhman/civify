package com.app.civify.repository.blog;

import com.app.civify.entity.blog.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // Public queries — only published posts, ordered by most recent
    List<Blog> findByPublishedTrueOrderByPublishedAtDesc();
    List<Blog> findByPublishedTrueAndFeaturedTrueOrderByPublishedAtDesc();
    Optional<Blog> findBySlugAndPublishedTrue(String slug);

    // Sitemap support — just slugs of published posts
    @Query("SELECT b.slug FROM Blog b WHERE b.published = true ORDER BY b.publishedAt DESC")
    List<String> findAllPublishedSlugs();
}
