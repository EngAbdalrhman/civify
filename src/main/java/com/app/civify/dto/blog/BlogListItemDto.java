package com.app.civify.dto.blog;

import java.time.LocalDateTime;

public record BlogListItemDto(
        Long id,
        String title,
        String slug,
        String metaDescription,
        String metaKeywords,
        String coverImageUrl,
        String readingTime,
        Boolean published,
        Boolean featured,
        LocalDateTime publishedAt,
        String authorName
) {
}
