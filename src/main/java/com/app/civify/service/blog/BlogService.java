package com.app.civify.service.blog;

import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.dto.blog.CreateBlogRequest;
import com.app.civify.dto.blog.UpdateBlogRequest;
import com.app.civify.entity.blog.Blog;
import com.app.civify.exception.ResourceAlreadyExistsException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.blog.BlogRepository;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.slug.SlugService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogService {
    private final BlogRepository blogRepository;
    private final SlugService slugService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "publicBlogs-v3", allEntries = true),
            @CacheEvict(value = "publicBlogs-v2", allEntries = true),
            @CacheEvict(value = "publicBlogs", allEntries = true)
    })
    public BlogDetailsDto create(CreateBlogRequest req, UserPrincipal author) {
        String base = slugService.slugify(req.title());
        String slug;
        if (req.slug() != null && !req.slug().isEmpty()) {
            if (blogRepository.existsBySlug(req.slug()))
                throw new ResourceAlreadyExistsException("slug already exist");
            slug = req.slug();
        } else {
            slug = slugService.uniqueSlug(base, blogRepository::existsBySlug);
        }

        Boolean isPublished = Boolean.TRUE.equals(req.published());

        Blog blog = Blog.builder()
                .title(req.title())
                .slug(slug)
                .content(req.content())
                .metaDescription(req.metaDescription())
                .metaKeywords(req.metaKeywords())
                .coverImageUrl(req.coverImageUrl())
                .readingTime(req.readingTime())
                .published(isPublished)
                .featured(Boolean.TRUE.equals(req.featured()))
                .publishedAt(isPublished ? LocalDateTime.now() : null)
                .author(author.getUser())
                .authorName(req.author() != null && !req.author().isBlank() ? req.author() : "Civify Team")
                .build();

        Blog saved = blogRepository.save(blog);
        return toDetailsDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "publicBlogs-v3", allEntries = true),
            @CacheEvict(value = "publicBlogs-v2", allEntries = true),
            @CacheEvict(value = "publicBlogs", allEntries = true)
    })
    public BlogDetailsDto update(Long id, UpdateBlogRequest req) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));

        if (req.title() != null) blog.setTitle(req.title());
        if (req.content() != null) blog.setContent(req.content());
        if (req.metaDescription() != null) blog.setMetaDescription(req.metaDescription());
        if (req.metaKeywords() != null) blog.setMetaKeywords(req.metaKeywords());
        if (req.coverImageUrl() != null) blog.setCoverImageUrl(req.coverImageUrl());
        if (req.readingTime() != null) blog.setReadingTime(req.readingTime());
        if (req.featured() != null) blog.setFeatured(req.featured());
        if (req.author() != null) blog.setAuthorName(req.author());

        if (req.slug() != null && !req.slug().isEmpty() && !req.slug().equals(blog.getSlug())) {
            if (blogRepository.existsBySlug(req.slug()))
                throw new ResourceAlreadyExistsException("slug already exists");
            blog.setSlug(req.slug());
        }

        // Handle publish/unpublish
        if (req.published() != null) {
            boolean wasPublished = Boolean.TRUE.equals(blog.getPublished());
            boolean nowPublished = req.published();
            blog.setPublished(nowPublished);
            if (nowPublished && !wasPublished) {
                blog.setPublishedAt(LocalDateTime.now());
            } else if (!nowPublished) {
                blog.setPublishedAt(null);
            }
        }

        Blog saved = blogRepository.save(blog);
        return toDetailsDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "publicBlogs-v3", allEntries = true),
            @CacheEvict(value = "publicBlogs-v2", allEntries = true),
            @CacheEvict(value = "publicBlogs", allEntries = true)
    })
    public void delete(Long id) {
        if (!blogRepository.existsById(id))
            throw new ResourceNotFoundException("Blog not found");
        blogRepository.deleteById(id);
    }

    public List<BlogListItemDto> list() {
        return blogRepository.findAll().stream()
                .map(this::toListItemDto)
                .toList();
    }

    public List<BlogListItemDto> listPublished() {
        return blogRepository.findByPublishedTrueOrderByPublishedAtDesc().stream()
                .map(this::toListItemDto)
                .toList();
    }

    public List<BlogListItemDto> listFeatured() {
        return blogRepository.findByPublishedTrueAndFeaturedTrueOrderByPublishedAtDesc().stream()
                .map(this::toListItemDto)
                .toList();
    }

    /**
     * Public-safe slug lookup — only returns published posts.
     * Prevents draft/unpublished posts from leaking via the public API.
     */
    public BlogDetailsDto getPublishedBySlug(String slug) {
        Blog b = blogRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));
        return toDetailsDto(b);
    }

    /**
     * Returns all published blog slugs for sitemap generation.
     */
    public List<String> getAllPublishedSlugs() {
        return blogRepository.findAllPublishedSlugs();
    }

    public BlogDetailsDto getBySlug(String slug) {
        Blog b = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));
        return toDetailsDto(b);
    }

    public BlogDetailsDto getById(Long id) {
        Blog b = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));
        return toDetailsDto(b);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private BlogDetailsDto toDetailsDto(Blog b) {
        String name = b.getAuthorName() != null && !b.getAuthorName().isBlank() ? b.getAuthorName() : 
                      (b.getAuthor() != null ? b.getAuthor().getName() : "Civify Team");
        return new BlogDetailsDto(
                b.getId(),
                b.getTitle(),
                b.getSlug(),
                b.getContent(),
                b.getMetaDescription(),
                b.getMetaKeywords(),
                b.getCoverImageUrl(),
                b.getReadingTime(),
                b.getPublished(),
                b.getFeatured(),
                b.getPublishedAt(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                name
        );
    }

    private BlogListItemDto toListItemDto(Blog b) {
        String name = b.getAuthorName() != null && !b.getAuthorName().isBlank() ? b.getAuthorName() : 
                      (b.getAuthor() != null ? b.getAuthor().getName() : "Civify Team");
        return new BlogListItemDto(
                b.getId(),
                b.getTitle(),
                b.getSlug(),
                b.getMetaDescription(),
                b.getMetaKeywords(),
                b.getCoverImageUrl(),
                b.getReadingTime(),
                b.getPublished(),
                b.getFeatured(),
                b.getPublishedAt(),
                name
        );
    }
}
