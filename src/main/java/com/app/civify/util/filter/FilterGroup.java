package com.app.civify.util.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter group for combining multiple filters with logical operations Supports
 * nested groups for complex filtering logic
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterGroup {

    private List<FilterCriteria> filters = new ArrayList<>();
    private List<FilterGroup> groups = new ArrayList<>();
    private LogicalOperation operation = LogicalOperation.AND; // Default is AND

    // Convenience methods for adding filters
    public FilterGroup addFilter(FilterCriteria filter) {
        this.filters.add(filter);
        return this;
    }

    public FilterGroup addFilter(String fieldName, FilterOperation operation, Object value) {
        this.filters.add(new FilterCriteria(fieldName, operation, value));
        return this;
    }

    public FilterGroup addFilter(String fieldName, FilterOperation operation, Object value, Object value2) {
        this.filters.add(new FilterCriteria(fieldName, operation, value, value2));
        return this;
    }

    // Convenience methods for adding groups
    public FilterGroup addGroup(FilterGroup group) {
        this.groups.add(group);
        return this;
    }

    public FilterGroup setLogicalOperation(LogicalOperation operation) {
        this.operation = operation;
        return this;
    }

    // Check if group has any filters or sub-groups
    public boolean isEmpty() {
        return (filters == null || filters.isEmpty()) && (groups == null || groups.isEmpty());
    }

    // Manual getters (Lombok not working properly)
    public List<FilterCriteria> getFilters() {
        return filters;
    }

    public List<FilterGroup> getGroups() {
        return groups;
    }

    public LogicalOperation getOperation() {
        return operation;
    }

    // Static factory methods
    public static FilterGroup and() {
        return new FilterGroup().setLogicalOperation(LogicalOperation.AND);
    }

    public static FilterGroup or() {
        return new FilterGroup().setLogicalOperation(LogicalOperation.OR);
    }
}
