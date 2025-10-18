package com.app.civify.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for filtered entity results with pagination metadata
 *
 * @param <T> Entity type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterResponse<T> {

    private List<T> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;

    /**
     * Create response from Spring Data Page
     */
    public static <T> FilterResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return FilterResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
