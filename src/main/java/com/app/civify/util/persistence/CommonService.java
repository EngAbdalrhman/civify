package com.app.civify.util.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import com.app.civify.util.filter.*;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common service for handling filtering, sorting, and pagination Provides
 * reusable methods for all entities using InputDataModel
 */
@Slf4j
@Service
public class CommonService {

    /**
     * Generic method to filter entities using InputDataModel
     *
     * @param repository The JPA repository
     * @param inputData The input data model containing filters, sorts, and
     * pagination
     * @return Page of filtered entities
     */
    public <T> Page<T> filterEntities(JpaSpecificationExecutor<T> repository, InputDataModel inputData) {

        Specification<T> spec = createSpecificationFromInputData(inputData);

        Sort sort = createSort(inputData.getSorts());

        Pageable pageable = PageRequest.of(inputData.getPage(), inputData.getSize(), sort);

        return repository.findAll(spec, pageable);
    }

    /**
     * Generic method to count entities using InputDataModel filters
     */
    public <T> long countEntities(JpaSpecificationExecutor<T> repository, InputDataModel inputData) {
        Specification<T> spec = createSpecificationFromInputData(inputData);
        return repository.count(spec);
    }

    /**
     * Generic method to check if entities exist using InputDataModel filters
     */
    public <T> boolean existsEntities(JpaSpecificationExecutor<T> repository, InputDataModel inputData) {
        return countEntities(repository, inputData) > 0;
    }

    /**
     * Create specification from InputDataModel (handles filters + filterGroups)
     * Structure: filters (AND) + filterGroups with operation between them
     */
    private <T> Specification<T> createSpecificationFromInputData(InputDataModel inputData) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> allPredicates = new ArrayList<>();

            // Add predicates from direct filters (always AND between them)
            if (inputData.getFilters() != null && !inputData.getFilters().isEmpty()) {
                List<Predicate> filterPredicates = new ArrayList<>();
                for (FilterCriteria filter : inputData.getFilters()) {
                    Predicate predicate = createPredicate(root, criteriaBuilder, filter);
                    if (predicate != null) {
                        filterPredicates.add(predicate);
                    }
                }
                if (!filterPredicates.isEmpty()) {
                    allPredicates.add(criteriaBuilder.and(filterPredicates.toArray(new Predicate[0])));
                }
            }

            // Add predicates from filter groups
            if (inputData.getFilterGroups() != null && !inputData.getFilterGroups().isEmpty()) {
                for (FilterGroup group : inputData.getFilterGroups()) {
                    Specification<T> groupSpec = createSpecificationFromGroup(group);
                    Predicate groupPredicate = groupSpec.toPredicate(root, query, criteriaBuilder);
                    if (groupPredicate != null) {
                        allPredicates.add(groupPredicate);
                    }
                }
            }

            if (allPredicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            // Combine all predicates based on operation
            if (inputData.getOperation() == LogicalOperation.OR) {
                return criteriaBuilder.or(allPredicates.toArray(new Predicate[0]));
            } else {
                return criteriaBuilder.and(allPredicates.toArray(new Predicate[0]));
            }
        };
    }

    /**
     * Create specification from FilterGroup (supports nested groups)
     */
    private <T> Specification<T> createSpecificationFromGroup(FilterGroup filterGroup) {
        return (root, query, criteriaBuilder) -> {
            if (filterGroup == null || filterGroup.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            List<Predicate> predicates = new ArrayList<>();

            // Add predicates from direct filters
            if (filterGroup.getFilters() != null && !filterGroup.getFilters().isEmpty()) {
                for (FilterCriteria filter : filterGroup.getFilters()) {
                    Predicate predicate = createPredicate(root, criteriaBuilder, filter);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }

            // Add predicates from nested groups
            if (filterGroup.getGroups() != null && !filterGroup.getGroups().isEmpty()) {
                for (FilterGroup nestedGroup : filterGroup.getGroups()) {
                    Specification<T> nestedSpec = createSpecificationFromGroup(nestedGroup);
                    Predicate nestedPredicate = nestedSpec.toPredicate(root, query, criteriaBuilder);
                    if (nestedPredicate != null) {
                        predicates.add(nestedPredicate);
                    }
                }
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            // Combine predicates based on logical operation
            if (filterGroup.getOperation() == LogicalOperation.OR) {
                return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
            } else {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    /**
     * Create specification from filters list and logical operation
     */
    private <T> Specification<T> createSpecification(List<FilterCriteria> filters, LogicalOperation operation) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filters != null && !filters.isEmpty()) {
                for (FilterCriteria filter : filters) {
                    Predicate predicate = createPredicate(root, criteriaBuilder, filter);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            if (operation == LogicalOperation.OR) {
                return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
            } else {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    /**
     * Create predicate for individual filter criteria
     */
    @SuppressWarnings("unchecked")
    private <T> Predicate createPredicate(jakarta.persistence.criteria.Root<T> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            FilterCriteria filter) {
        try {
            String fieldName = filter.getFieldName();
            FilterOperation operation = filter.getOperation();
            Object rawValue = filter.getValue();
            Object rawValue2 = filter.getValue2();

            // Handle nested fields (e.g., "user.id")
            jakarta.persistence.criteria.Path<Object> path = getPath(root, fieldName);
            Class<?> targetType = path.getJavaType();

            // Convert values to the target type
            Object value = convertValue(rawValue, targetType);
            Object value2 = convertValue(rawValue2, targetType);

            if (operation == FilterOperation.EQUAL) {
                return criteriaBuilder.equal(path, value);
            } else if (operation == FilterOperation.NOT_EQUAL) {
                return criteriaBuilder.notEqual(path, value);
            } else if (operation == FilterOperation.MATCH) {
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        "%" + String.valueOf(value).toLowerCase() + "%"
                );
            } else if (operation == FilterOperation.MATCH_START) {
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        String.valueOf(value).toLowerCase() + "%"
                );
            } else if (operation == FilterOperation.MATCH_END) {
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        "%" + String.valueOf(value).toLowerCase()
                );
            } else if (operation == FilterOperation.MATCH_EXACT) {
                return criteriaBuilder.equal(path.as(String.class), String.valueOf(value));
            } else if (operation == FilterOperation.GREATER_THAN) {
                return criteriaBuilder.greaterThan(path.as(Comparable.class), (Comparable) value);
            } else if (operation == FilterOperation.GREATER_THAN_EQUAL) {
                return criteriaBuilder.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            } else if (operation == FilterOperation.LESS_THAN) {
                return criteriaBuilder.lessThan(path.as(Comparable.class), (Comparable) value);
            } else if (operation == FilterOperation.LESS_THAN_EQUAL) {
                return criteriaBuilder.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            } else if (operation == FilterOperation.BETWEEN) {
                if (value2 != null) {
                    return criteriaBuilder.between(path.as(Comparable.class), (Comparable) value, (Comparable) value2);
                }
            } else if (operation == FilterOperation.IN_LONG_LIST || operation == FilterOperation.IN_STRING_LIST || operation == FilterOperation.IN_INTEGER_LIST) {
                if (rawValue instanceof Collection) {
                    // For IN clauses, we need to convert each element
                    Collection<?> rawCollection = (Collection<?>) rawValue;
                    List<Object> convertedCollection = new ArrayList<>();
                    for (Object item : rawCollection) {
                        convertedCollection.add(convertValue(item, targetType));
                    }
                    return path.in(convertedCollection);
                }
            } else if (operation == FilterOperation.NOT_IN_LONG_LIST || operation == FilterOperation.NOT_IN_STRING_LIST || operation == FilterOperation.NOT_IN_INTEGER_LIST) {
                if (rawValue instanceof Collection) {
                    Collection<?> rawCollection = (Collection<?>) rawValue;
                    List<Object> convertedCollection = new ArrayList<>();
                    for (Object item : rawCollection) {
                        convertedCollection.add(convertValue(item, targetType));
                    }
                    return criteriaBuilder.not(path.in(convertedCollection));
                }
            } else if (operation == FilterOperation.IS_NULL) {
                return criteriaBuilder.isNull(path);
            } else if (operation == FilterOperation.IS_NOT_NULL) {
                return criteriaBuilder.isNotNull(path);
            } else {
                log.warn("Unsupported filter operation: {}", operation);
            }
        } catch (Exception e) {
            log.error("Error creating predicate for filter {}: {}", filter.getFieldName(), e.getMessage());
        }

        return null;
    }

    /**
     * Get path for nested fields
     */
    private <T> jakarta.persistence.criteria.Path<Object> getPath(jakarta.persistence.criteria.Root<T> root, String fieldName) {
        String[] parts = fieldName.split("\\.");
        jakarta.persistence.criteria.Path<Object> path = root.get(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }

        return path;
    }

    /**
     * Create sort from sort criteria list
     */
    private Sort createSort(List<SortCriteria> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (SortCriteria sortCriteria : sorts) {
            Sort.Direction direction = sortCriteria.getDirection() == SortDirection.DESC
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, sortCriteria.getFieldName()));
        }

        return Sort.by(orders);
    }

    /**
     * Convert LocalDate to LocalDateTime for filtering
     */
    public LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * Convert LocalDate to end of day LocalDateTime for filtering
     */
    public LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : null;
    }

    /**
     * Helper to convert value to target type
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        String stringValue = String.valueOf(value);

        if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.valueOf(stringValue);
        } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.valueOf(stringValue);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.valueOf(stringValue);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.valueOf(stringValue);
        } else if (targetType.equals(LocalDateTime.class)) {
            return LocalDateTime.parse(stringValue); // Assumes ISO format
        } else if (targetType.equals(LocalDate.class)) {
            return LocalDate.parse(stringValue);
        } else if (Enum.class.isAssignableFrom(targetType)) {
             // Handle Enums
            return Enum.valueOf((Class<Enum>) targetType, stringValue);
        }

        return value;
    }
}
