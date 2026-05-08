package com.app.civify.dto.blog;

public record CreateBlogRequest(
        String title,
        String content,
        String slug,
        String metaDescription,
        String metaKeywords,
        String coverImageUrl,
        String readingTime,
        Boolean published,
        Boolean featured
) {
}
