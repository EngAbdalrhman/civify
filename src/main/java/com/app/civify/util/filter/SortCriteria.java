package com.app.civify.util.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sort criteria for ordering results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortCriteria {

    private String fieldName;
    private SortDirection direction = SortDirection.ASC;

    public SortCriteria(String fieldName) {
        this.fieldName = fieldName;
        this.direction = SortDirection.ASC;
    }

    // Manual getters (Lombok not working properly)
    public String getFieldName() {
        return fieldName;
    }

    public SortDirection getDirection() {
        return direction;
    }
}
