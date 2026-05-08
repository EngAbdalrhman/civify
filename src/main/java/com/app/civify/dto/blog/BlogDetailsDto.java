package com.app.civify.dto.blog;

import java.time.LocalDateTime;

public record BlogDetailsDto(
        Long id,
        String title,
        String slug,
        String content,
        String metaDescription,
        String metaKeywords,
        String coverImageUrl,
        String readingTime,
        Boolean published,
        Boolean featured,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String authorName
) {
}
