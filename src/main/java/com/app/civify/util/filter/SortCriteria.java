package com.app.civify.util.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sort criteria for ordering results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class SortCriteria {

    private String fieldName;
    @Builder.Default
    private SortDirection direction = SortDirection.ASC;

    public SortCriteria(String fieldName) {
        this.fieldName = fieldName;
        this.direction = SortDirection.ASC;
    }

}
