package com.app.civify.service.filter;

import com.app.civify.util.filter.FilterCriteria;
import com.app.civify.util.filter.FilterGroup;
import com.app.civify.util.filter.FilterOperation;
import com.app.civify.util.filter.LogicalOperation;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic filter service that converts FilterGroup to JPA Specifications
 * Works with any entity and supports complex nested filtering logic
 *
 * Usage:
 * <pre>
 * FilterGroup group = FilterGroup.and()
 *     .addFilter("name", FilterOperation.MATCH, "John")
 *     .addFilter("age", FilterOperation.GREATER_THAN, 18);
 *
 * Specification<User> spec = genericFilterService.buildSpecification(group);
 * List<User> users = userRepository.findAll(spec);
 * </pre>
 */
@Service
public class GenericFilterService {

    /**
     * Build JPA Specification from FilterGroup
     */
    public <T> Specification<T> buildSpecification(FilterGroup filterGroup) {
        if (filterGroup == null || filterGroup.isEmpty()) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
            buildPredicate(filterGroup, root, query, criteriaBuilder);
    }

    /**
     * Build predicate from filter group (recursive for nested groups)
     */
    private <T> Predicate buildPredicate(FilterGroup group, Root<T> root,
                                         CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Process individual filters
        if (group.getFilters() != null) {
            for (FilterCriteria criteria : group.getFilters()) {
                Predicate predicate = buildPredicate(criteria, root, cb);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
        }

        // Process nested groups (recursive)
        if (group.getGroups() != null) {
            for (FilterGroup nestedGroup : group.getGroups()) {
                Predicate nestedPredicate = buildPredicate(nestedGroup, root, query, cb);
                if (nestedPredicate != null) {
                    predicates.add(nestedPredicate);
                }
            }
        }

        if (predicates.isEmpty()) {
            return null;
        }

        // Combine predicates based on logical operation
        LogicalOperation operation = group.getOperation();
        if (operation == LogicalOperation.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else if (operation == LogicalOperation.NOT) {
            return cb.not(cb.and(predicates.toArray(new Predicate[0])));
        } else {
            // Default: AND
            return cb.and(predicates.toArray(new Predicate[0]));
        }
    }

    /**
     * Build predicate from individual filter criteria
     */
    private <T> Predicate buildPredicate(FilterCriteria criteria, Root<T> root, CriteriaBuilder cb) {
        String fieldName = criteria.getFieldName();
        FilterOperation operation = criteria.getOperation();
        Object value = criteria.getValue();
        Object value2 = criteria.getValue2();

        // Handle nested fields (e.g., "user.email")
        Path<?> path = getPath(root, fieldName);

        switch (operation) {
            // Comparison operations
            case EQUAL:
                return cb.equal(path, value);
            case NOT_EQUAL:
                return cb.notEqual(path, value);
            case GREATER_THAN:
                return cb.greaterThan(path.as(Comparable.class), (Comparable) value);
            case GREATER_THAN_EQUAL:
                return cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case LESS_THAN:
                return cb.lessThan(path.as(Comparable.class), (Comparable) value);
            case LESS_THAN_EQUAL:
                return cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);

            // String operations
            case MATCH:
                return cb.like(cb.lower(path.as(String.class)),
                    "%" + value.toString().toLowerCase() + "%");
            case MATCH_START:
                return cb.like(cb.lower(path.as(String.class)),
                    value.toString().toLowerCase() + "%");
            case MATCH_END:
                return cb.like(cb.lower(path.as(String.class)),
                    "%" + value.toString().toLowerCase());
            case MATCH_EXACT:
                return cb.equal(path, value);
            case MATCH_REGEX:
                // PostgreSQL regex (adjust for other databases)
                return cb.isTrue(cb.function("regexp_like", Boolean.class,
                    path, cb.literal(value.toString())));

            // Null operations
            case IS_NULL:
                return cb.isNull(path);
            case IS_NOT_NULL:
                return cb.isNotNull(path);

            // Collection operations
            case IN_LONG_LIST:
                return path.in((Collection<?>) value);
            case NOT_IN_LONG_LIST:
                return cb.not(path.in((Collection<?>) value));
            case IN_STRING_LIST:
                return path.in((Collection<?>) value);
            case NOT_IN_STRING_LIST:
                return cb.not(path.in((Collection<?>) value));
            case IN_INTEGER_LIST:
                return path.in((Collection<?>) value);
            case NOT_IN_INTEGER_LIST:
                return cb.not(path.in((Collection<?>) value));

            // Range operations
            case BETWEEN:
                return cb.between(path.as(Comparable.class), (Comparable) value, (Comparable) value2);
            case NOT_BETWEEN:
                return cb.not(cb.between(path.as(Comparable.class), (Comparable) value, (Comparable) value2));

            // Date operations
            case DATE_GREATER_THAN:
                return cb.greaterThan(path.as(LocalDateTime.class), parseDateTime(value));
            case DATE_GREATER_THAN_EQUAL:
                return cb.greaterThanOrEqualTo(path.as(LocalDateTime.class), parseDateTime(value));
            case DATE_LESS_THAN:
                return cb.lessThan(path.as(LocalDateTime.class), parseDateTime(value));
            case DATE_LESS_THAN_EQUAL:
                return cb.lessThanOrEqualTo(path.as(LocalDateTime.class), parseDateTime(value));
            case DATE_BETWEEN:
                return cb.between(path.as(LocalDateTime.class),
                    parseDateTime(value), parseDateTime(value2));
            case DATE_RANGE:
                return cb.between(path.as(LocalDateTime.class),
                    parseDateTime(value), parseDateTime(value2));
            case DAY_EQUAL:
                LocalDate targetDate = parseDate(value);
                LocalDateTime startOfDay = targetDate.atStartOfDay();
                LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
                return cb.between(path.as(LocalDateTime.class), startOfDay, endOfDay);

            // Full text search (PostgreSQL specific)
            case FULL_TEXT_SEARCH:
                return cb.isTrue(cb.function("to_tsvector", Boolean.class,
                    cb.literal("english"), path)
                    .in(cb.function("to_tsquery", Object.class,
                        cb.literal("english"), cb.literal(value.toString()))));

            default:
                throw new IllegalArgumentException("Unsupported filter operation: " + operation);
        }
    }

    /**
     * Get path for nested fields (e.g., "user.email" -> root.get("user").get("email"))
     */
    private <T> Path<?> getPath(Root<T> root, String fieldName) {
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            Path<?> path = root.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            return path;
        }
        return root.get(fieldName);
    }

    /**
     * Parse date/time value from various formats
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay();
        }
        if (value instanceof String) {
            String str = (String) value;
            // Try different formats
            try {
                return LocalDateTime.parse(str);
            } catch (Exception e) {
                try {
                    return LocalDate.parse(str).atStartOfDay();
                } catch (Exception e2) {
                    try {
                        return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
                    } catch (Exception e3) {
                        throw new IllegalArgumentException("Cannot parse date: " + str);
                    }
                }
            }
        }
        throw new IllegalArgumentException("Invalid date value: " + value);
    }

    /**
     * Parse date value from various formats
     */
    private LocalDate parseDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        }
        if (value instanceof String) {
            return LocalDate.parse((String) value);
        }
        throw new IllegalArgumentException("Invalid date value: " + value);
    }
}
