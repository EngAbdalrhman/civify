package com.app.civify.util.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Input data model for filtering, sorting, and pagination Simple structure:
 * filters (default AND) + filterGroups with operations between them
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputDataModel {

    private List<FilterCriteria> filters = new ArrayList<>(); // Default filters (AND operation)
    private List<FilterGroup> filterGroups = new ArrayList<>(); // Additional filter groups
    private LogicalOperation operation = LogicalOperation.AND; // Operation between groups (default AND)
    private List<SortCriteria> sorts = new ArrayList<>();
    private int page = 0;
    private int size = 20; // offset

    // Convenience methods for adding filters
    public InputDataModel addFilter(String fieldName, FilterOperation operation, Object value) {
        this.filters.add(new FilterCriteria(fieldName, operation, value));
        return this;
    }

    public InputDataModel addFilter(String fieldName, FilterOperation operation, Object value, Object value2) {
        this.filters.add(new FilterCriteria(fieldName, operation, value, value2));
        return this;
    }

    // Convenience methods for adding filter groups
    public InputDataModel addFilterGroup(FilterGroup group) {
        this.filterGroups.add(group);
        return this;
    }

    public InputDataModel addSort(String fieldName, SortDirection direction) {
        this.sorts.add(new SortCriteria(fieldName, direction));
        return this;
    }

    public InputDataModel addSort(String fieldName) {
        this.sorts.add(new SortCriteria(fieldName, SortDirection.ASC));
        return this;
    }

    public InputDataModel setOperation(LogicalOperation operation) {
        this.operation = operation;
        return this;
    }

    public InputDataModel setPagination(int page, int size) {
        this.page = page;
        this.size = size;
        return this;
    }

    // Manual getters (Lombok not working properly)
    public List<FilterCriteria> getFilters() {
        return filters;
    }

    public List<FilterGroup> getFilterGroups() {
        return filterGroups;
    }

    public LogicalOperation getOperation() {
        return operation;
    }

    public List<SortCriteria> getSorts() {
        return sorts;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
