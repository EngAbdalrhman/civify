package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.dto.blog.CreateBlogRequest;
import com.app.civify.dto.blog.UpdateBlogRequest;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.blog.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/blogs")
@Tag(name = "Admin Blog Management", description = "Admin endpoints for creating and managing blog posts")
public class AdminBlogController {

    private final BlogService blogService;

    @Operation(
            summary = "Create a new blog post",
            description = "Creates a new blog post with optional SEO metadata. Requires CREATE.BLOG permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog post created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogDetailsDto.class))),
            @ApiResponse(responseCode = "409", description = "Slug already exists",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/")
    @CivifyAuthorization(permission = "CREATE.BLOG")
    public BlogDetailsDto create(
            @RequestBody CreateBlogRequest req,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return blogService.create(req, userPrincipal);
    }

    @Operation(
            summary = "Update an existing blog post",
            description = "Partially updates a blog post by ID. All fields are optional — only provided fields are updated. Requires UPDATE.BLOG permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog post updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Slug already exists",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    @CivifyAuthorization(permission = "UPDATE.BLOG")
    public BlogDetailsDto update(
            @Parameter(description = "Blog post ID", required = true) @PathVariable Long id,
            @RequestBody UpdateBlogRequest req) {
        return blogService.update(id, req);
    }

    @Operation(
            summary = "Delete a blog post",
            description = "Permanently deletes a blog post by ID. Requires DELETE.BLOG permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Blog post deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CivifyAuthorization(permission = "DELETE.BLOG")
    public void delete(
            @Parameter(description = "Blog post ID", required = true) @PathVariable Long id) {
        blogService.delete(id);
    }

    @Operation(
            summary = "List all blog posts (admin)",
            description = "Returns all blog posts including unpublished drafts. Requires VIEW.BLOG permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog posts retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogListItemDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/")
    @CivifyAuthorization(permission = "VIEW.BLOG")
    public List<BlogListItemDto> listAll() {
        return blogService.list();
    }

    @Operation(
            summary = "Get blog post by ID (admin)",
            description = "Returns full blog post details including draft content. Requires VIEW.BLOG permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blog post retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Blog post not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    @CivifyAuthorization(permission = "VIEW.BLOG")
    public BlogDetailsDto getById(
            @Parameter(description = "Blog post ID", required = true) @PathVariable Long id) {
        return blogService.getById(id);
    }
}
