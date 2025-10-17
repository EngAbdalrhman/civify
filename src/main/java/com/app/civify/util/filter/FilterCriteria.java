package com.app.civify.util.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for individual field filtering
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {

    private String fieldName;
    private FilterOperation operation;
    private Object value;
    private Object value2; // For BETWEEN operations

    public FilterCriteria(String fieldName, FilterOperation operation, Object value) {
        this.fieldName = fieldName;
        this.operation = operation;
        this.value = value;
    }

    // Manual getters (Lombok not working properly)
    public String getFieldName() {
        return fieldName;
    }

    public FilterOperation getOperation() {
        return operation;
    }

    public Object getValue() {
        return value;
    }

    public Object getValue2() {
        return value2;
    }
}
