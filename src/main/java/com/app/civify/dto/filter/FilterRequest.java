package com.app.civify.dto.filter;

import com.app.civify.util.filter.FilterGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * Request DTO for filtering entities with pagination and sorting
 *
 * Usage example:
 * <pre>
 * {
 *   "filterGroup": {
 *     "filters": [
 *       {"fieldName": "endpoint", "operation": "MATCH", "value": "/api/v1/cv"},
 *       {"fieldName": "success", "operation": "EQUAL", "value": true}
 *     ],
 *     "operation": "AND"
 *   },
 *   "page": 0,
 *   "size": 20,
 *   "sortBy": "timestamp",
 *   "sortDirection": "DESC"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {

    private FilterGroup filterGroup;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    private String sortBy;

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;

    public Sort getSort() {
        if (sortBy != null && !sortBy.isEmpty()) {
            return Sort.by(sortDirection, sortBy);
        }
        return Sort.unsorted();
    }
}
