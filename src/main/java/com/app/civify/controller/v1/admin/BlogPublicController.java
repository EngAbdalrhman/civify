package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.service.blog.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiV1Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/blogs")
@Tag(name = "Blog", description = "Public blog endpoints for SEO-friendly content delivery")
public class BlogPublicController {

    private final BlogService blogService;

    @Operation(
            summary = "List all published blog posts",
            description = "Returns all published blog posts with SEO metadata (title, slug, meta description, cover image). Suitable for blog index pages and sitemaps."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Published blog posts retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogListItemDto.class)))
    })
    @GetMapping("/")
    @Cacheable("publicBlogs")
    public List<BlogListItemDto> list() {
        return blogService.listPublished();
    }

    @Operation(
            summary = "List featured blog posts",
            description = "Returns only published and featured blog posts. Useful for homepage highlights and editorial picks."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Featured blog posts retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogListItemDto.class)))
    })
    @GetMapping("/featured")
    @Cacheable("publicBlogs")
    public List<BlogListItemDto> listFeatured() {
        return blogService.listFeatured();
    }

    @Operation(
            summary = "Get published blog post by slug",
            description = "Returns the full content of a published blog post by its SEO-friendly slug. Unpublished/draft posts return 404."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog post retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found or not published",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{slug}")
    @Cacheable(value = "publicBlogs", key = "#slug")
    public BlogDetailsDto getBySlug(
            @Parameter(description = "SEO-friendly slug of the blog post", example = "how-to-build-a-great-cv", required = true)
            @PathVariable String slug) {
        return blogService.getPublishedBySlug(slug);
    }

    @Operation(
            summary = "Get all published blog slugs",
            description = "Returns a list of slugs for all published posts. Used for sitemap generation and static page pre-rendering."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Slugs retrieved successfully")
    })
    @GetMapping("/slugs")
    @Cacheable("publicBlogs")
    public List<String> getSlugs() {
        return blogService.getAllPublishedSlugs();
    }
}

