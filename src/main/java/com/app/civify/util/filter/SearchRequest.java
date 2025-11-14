package com.app.civify.util.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified search request model for easier filtering
 * This is a cleaner alternative to InputDataModel
 * 
 * Example usage:
 * {
 *   "filters": {
 *     "email": "example.com",           // Simple MATCH operation
 *     "role": "ROLE_USER",              // Simple EQUAL operation
 *     "isEmailVerified": true           // Simple EQUAL operation
 *   },
 *   "sort": [
 *     {"field": "createdAt", "direction": "DESC"},
 *     {"field": "email", "direction": "ASC"}
 *   ],
 *   "page": 0,
 *   "size": 10
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simplified search request with filters, sorting, and pagination")
public class SearchRequest {
    
    @Schema(description = "Simple key-value filters (uses MATCH for strings, EQUAL for others)", example = "{\"email\": \"example.com\", \"role\": \"ROLE_USER\"}")
    private java.util.Map<String, Object> filters;
    
    @Schema(description = "Advanced filters with specific operations")
    private List<FilterCriteria> advancedFilters;
    
    @Schema(description = "Logical operation between filters (AND/OR)", example = "AND", defaultValue = "AND")
    @Builder.Default
    private LogicalOperation operation = LogicalOperation.AND;
    
    @Schema(description = "Sort criteria")
    private List<SortCriteria> sort;
    
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    @Builder.Default
    private int page = 0;
    
    @Schema(description = "Page size (number of items per page)", example = "10", defaultValue = "10")
    @Builder.Default
    private int size = 10;
    
    /**
     * Convert to InputDataModel for backward compatibility
     */
    public InputDataModel toInputDataModel() {
        InputDataModel inputData = new InputDataModel();
        inputData.setPage(this.page);
        inputData.setSize(this.size);
        inputData.setOperation(this.operation);
        
        // Convert simple filters to FilterCriteria
        if (filters != null && !filters.isEmpty()) {
            List<FilterCriteria> criteriaList = new ArrayList<>();
            for (java.util.Map.Entry<String, Object> entry : filters.entrySet()) {
                FilterOperation op = determineOperation(entry.getValue());
                criteriaList.add(new FilterCriteria(entry.getKey(), op, entry.getValue()));
            }
            inputData.setFilters(criteriaList);
        }
        
        // Add advanced filters
        if (advancedFilters != null && !advancedFilters.isEmpty()) {
            if (inputData.getFilters() == null) {
                inputData.setFilters(new ArrayList<>());
            }
            inputData.getFilters().addAll(advancedFilters);
        }
        
        // Convert sort
        if (sort != null && !sort.isEmpty()) {
            inputData.setSorts(sort);
        }
        
        return inputData;
    }
    
    /**
     * Determine operation based on value type
     */
    private FilterOperation determineOperation(Object value) {
        if (value instanceof String) {
            return FilterOperation.MATCH; // Use MATCH for strings (contains)
        } else if (value instanceof Boolean || value instanceof Number) {
            return FilterOperation.EQUAL; // Use EQUAL for booleans and numbers
        } else if (value instanceof List) {
            return FilterOperation.IN_STRING_LIST; // Use IN for lists
        }
        return FilterOperation.EQUAL; // Default to EQUAL
    }
}

