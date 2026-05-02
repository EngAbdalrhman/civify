package com.app.civify.util;

import com.app.civify.annotation.FilterField;
import com.app.civify.annotation.FilterFieldMapping;
import com.app.civify.util.filter.FilterCriteria;
import com.app.civify.util.filter.FilterGroup;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.SortCriteria;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FilterMappingResolver {
    private final Map<Class<?>, Map<String, String>> cache = new ConcurrentHashMap<>();


    public Map<String, String> getMappings(Class<?> entityClass) {
        return cache.computeIfAbsent(entityClass, this::buildMappings);
    }

    private Map<String, String> buildMappings(Class<?> entityClass) {
        FilterFieldMapping annotation = entityClass.getAnnotation(FilterFieldMapping.class);
        if (annotation == null) return Collections.emptyMap();

        Map<String, String> mappings = new HashMap<>();
        for (FilterField field : annotation.value()) {
            mappings.put(field.alias(), field.mapping());
        }
        return mappings;
    }


    public void applyMappings(InputDataModel inputData, Class<?> entityClass) {
        Map<String, String> mappings = getMappings(entityClass);
        if (mappings.isEmpty()) return;

        if (inputData.getFilters() != null) {
            inputData.getFilters().forEach(filter -> resolveField(filter, mappings));
        }

        if (inputData.getFilterGroups() != null) {
            inputData.getFilterGroups().forEach(group -> resolveGroup(group, mappings));
        }

        if (inputData.getSorts() != null) {
            inputData.getSorts().forEach(sort -> resolveSort(sort, mappings));
        }
    }

    private void resolveField(FilterCriteria filter, Map<String, String> mappings) {
        String mapped = mappings.get(filter.getFieldName());
        if (mapped != null) {
            filter.setFieldName(mapped);
        }
    }

    private void resolveSort(SortCriteria sort, Map<String, String> mappings) {
        String mapped = mappings.get(sort.getFieldName());
        if (mapped != null) {
            sort.setFieldName(mapped);
        }
    }

    private void resolveGroup(FilterGroup group, Map<String, String> mappings) {
        if (group.getFilters() != null) {
            group.getFilters().forEach(filter -> resolveField(filter, mappings));
        }
        if (group.getGroups() != null) {
            group.getGroups().forEach(nested -> resolveGroup(nested, mappings));
        }
    }

}

