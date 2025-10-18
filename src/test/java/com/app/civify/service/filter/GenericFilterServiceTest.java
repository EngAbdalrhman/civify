//package com.app.civify.service.filter;
//
//import com.app.civify.entity.ApiUsageLog;
//import com.app.civify.util.filter.FilterCriteria;
//import com.app.civify.util.filter.FilterGroup;
//import com.app.civify.util.filter.FilterOperation;
//import com.app.civify.util.filter.LogicalOperation;
//import jakarta.persistence.criteria.CriteriaBuilder;
//import jakarta.persistence.criteria.CriteriaQuery;
//import jakarta.persistence.criteria.Predicate;
//import jakarta.persistence.criteria.Root;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class GenericFilterServiceTest {
//
//    private GenericFilterService filterService;
//
//    @BeforeEach
//    void setUp() {
//        filterService = new GenericFilterService();
//    }
//
//    @Test
//    void buildSpecification_NullFilterGroup_ReturnsNull() {
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(null);
//
//        // Then
//        assertNull(spec);
//    }
//
//    @Test
//    void buildSpecification_EmptyFilterGroup_ReturnsNull() {
//        // Given
//        FilterGroup emptyGroup = new FilterGroup();
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(emptyGroup);
//
//        // Then
//        assertNull(spec);
//    }
//
//    @Test
//    void buildSpecification_SingleEqualFilter_Success() {
//        // Given
//        FilterGroup group = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.EQUAL, "/api/v1/cv");
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_MultipleFiltersWithAND_Success() {
//        // Given
//        FilterGroup group = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.MATCH, "/api/v1")
//                .addFilter("success", FilterOperation.EQUAL, true);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_MultipleFiltersWithOR_Success() {
//        // Given
//        FilterGroup group = FilterGroup.or()
//                .addFilter("endpoint", FilterOperation.MATCH, "/api/v1/cv")
//                .addFilter("endpoint", FilterOperation.MATCH, "/api/v1/payment");
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_NestedGroups_Success() {
//        // Given
//        FilterGroup innerGroup1 = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.MATCH, "/api/v1/cv");
//
//        FilterGroup innerGroup2 = FilterGroup.and()
//                .addFilter("success", FilterOperation.EQUAL, false);
//
//        FilterGroup outerGroup = FilterGroup.or()
//                .addGroup(innerGroup1)
//                .addGroup(innerGroup2);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(outerGroup);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_StringOperations_Success() {
//        // Given - Test all string operations
//        FilterGroup group = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.MATCH, "cv")
//                .addFilter("endpoint", FilterOperation.MATCH_START, "/api")
//                .addFilter("endpoint", FilterOperation.MATCH_END, "generate")
//                .addFilter("endpoint", FilterOperation.MATCH_EXACT, "/api/v1/cv/generate");
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_ComparisonOperations_Success() {
//        // Given
//        LocalDateTime date = LocalDateTime.now().minusDays(7);
//
//        FilterGroup group = FilterGroup.and()
//                .addFilter("timestamp", FilterOperation.GREATER_THAN, date)
//                .addFilter("timestamp", FilterOperation.LESS_THAN, LocalDateTime.now());
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_NullOperations_Success() {
//        // Given
//        FilterGroup group = FilterGroup.or()
//                .addFilter("errorMessage", FilterOperation.IS_NULL, null)
//                .addFilter("errorMessage", FilterOperation.IS_NOT_NULL, null);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_CollectionOperations_Success() {
//        // Given
//        FilterGroup group = FilterGroup.and()
//                .addFilter("id", FilterOperation.IN_LONG_LIST, Arrays.asList(1L, 2L, 3L))
//                .addFilter("endpoint", FilterOperation.IN_STRING_LIST,
//                        Arrays.asList("/api/v1/cv", "/api/v1/payment"));
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_BetweenOperation_Success() {
//        // Given
//        LocalDateTime start = LocalDateTime.now().minusDays(30);
//        LocalDateTime end = LocalDateTime.now();
//
//        FilterGroup group = FilterGroup.and()
//                .addFilter("timestamp", FilterOperation.DATE_BETWEEN, start, end);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_DateOperations_Success() {
//        // Given
//        LocalDateTime date = LocalDateTime.now().minusDays(7);
//
//        FilterGroup group = FilterGroup.and()
//                .addFilter("timestamp", FilterOperation.DATE_GREATER_THAN, date)
//                .addFilter("timestamp", FilterOperation.DATE_LESS_THAN_EQUAL, LocalDateTime.now());
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_NestedFieldAccess_Success() {
//        // Given - Access nested field like "user.email"
//        FilterGroup group = FilterGroup.and()
//                .addFilter("user.email", FilterOperation.MATCH, "test@example.com");
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_NotOperation_Success() {
//        // Given
//        FilterGroup group = new FilterGroup();
//        group.addFilter("success", FilterOperation.EQUAL, false);
//        group.setLogicalOperation(LogicalOperation.NOT);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_ComplexNestedFilters_Success() {
//        // Given - Complex scenario: (endpoint contains "cv" AND success = true)
//        //                        OR (endpoint contains "payment" AND success = false)
//        FilterGroup cvGroup = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.MATCH, "cv")
//                .addFilter("success", FilterOperation.EQUAL, true);
//
//        FilterGroup paymentGroup = FilterGroup.and()
//                .addFilter("endpoint", FilterOperation.MATCH, "payment")
//                .addFilter("success", FilterOperation.EQUAL, false);
//
//        FilterGroup mainGroup = FilterGroup.or()
//                .addGroup(cvGroup)
//                .addGroup(paymentGroup);
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(mainGroup);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_DayEqualOperation_Success() {
//        // Given
//        FilterGroup group = FilterGroup.and()
//                .addFilter("timestamp", FilterOperation.DAY_EQUAL, LocalDateTime.now());
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_NotInOperation_Success() {
//        // Given
//        FilterGroup group = FilterGroup.and()
//                .addFilter("id", FilterOperation.NOT_IN_LONG_LIST, Arrays.asList(1L, 2L, 3L));
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//
//    @Test
//    void buildSpecification_MultiLevelNestedFields_Success() {
//        // Given - Test deeply nested field access like "user.subscription.plan.name"
//        FilterGroup group = FilterGroup.and()
//                .addFilter("user.username", FilterOperation.EQUAL, "testuser");
//
//        // When
//        Specification<ApiUsageLog> spec = filterService.buildSpecification(group);
//
//        // Then
//        assertNotNull(spec);
//    }
//}
